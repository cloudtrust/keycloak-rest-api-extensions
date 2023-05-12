#!/bin/bash
# install.sh
#
# install keycloak module

set -eE -o pipefail
MODULE_DIR=$(dirname $0)
TARGET_DIR=$MODULE_DIR/target
#[ -z "$WINDIR" ] && KC_EXE=kc.sh || KC_EXE=kc.bat
MODULE_CONF=${MODULE_DIR}/module.conf

NBSOURCES=$(find ${TARGET_DIR} -name *sources.jar |wc -l)
#[ $NBSOURCES -ne 0 ] && echo "When running from source project, please use deploy-from-tar.sh" && exit 0

usage ()
{
    echo "usage: $0 /path/to/keycloak [-t host] [-u] [-b]"
}

abort_usage_keycloak()
{
  echo "Invalid keycloak path"
  usage
  exit 1
}

init()
{
    #optional args
    argv__TARGET="http://localhost:8811/event/receiver"
    argv__UNINSTALL=0
    getopt_results=$(getopt -s bash -o t:u --long target:,uninstall -- "$@")

    if test $? != 0
    then
        echo "unrecognized option"
        exit 1
    fi
    eval set -- "$getopt_results"

    while true
    do
        case "$1" in
            -u|--uninstall)
                argv__UNINSTALL=1
                echo "--uninstall set. will remove plugin"
                shift
                ;;
            -t|--target)
                argv__TARGET="$2"
                echo "--target set to \"$argv__TARGET\". Will edit the emitter target URI"
                shift 2
                ;;
            --)
                shift
                break
                ;;
            *)
                EXCEPTION=$Main__ParameterException
                EXCEPTION_MSG="unparseable option $1"
                exit 1
                ;;
        esac
    done

    # positional args
    argv__KEYCLOAK=""
    if [[ "$#" -ne 1 ]]; then
        usage
        exit 1
    fi
    argv__KEYCLOAK="$1"
    [ -d $argv__KEYCLOAK ] && [ -d $argv__KEYCLOAK/bin ] && [ -d $argv__KEYCLOAK/providers ] && [ -d $argv__KEYCLOAK/conf ] || abort_usage_keycloak
    # optional args
    CONF_FILE=$argv__KEYCLOAK/conf/keycloak.conf
    JAR_PATH=`find ${TARGET_DIR} -type f -name "*.jar" -not -name "*sources.jar" | grep -v "libs/"`
    JAR_NAME=`basename $JAR_PATH`
    if [ -z "${JAR_NAME}" ]; then
        echo "Can't get jar name"
        usage
        exit 1
    fi
}

init_exceptions()
{
    EXCEPTION=0
    EXCEPTION_MSG=""
    #Main__Default_Unkown=1
    Main__ParameterException=2
}

del_configuration()
{
  if [[ ! -z "$1" ]] ; then
    sed -i "/^$1=/d" ${CONF_FILE}
  fi
}

add_configuration()
{
  if [[ ! -z "$1" ]] ; then
    sed -i "/^$1=/d" ${CONF_FILE}
    echo "$1=$2" >> ${CONF_FILE}
  fi
}

cleanup_configuration()
{
    if [ -f "${MODULE_CONF}" ]; then
        for i in $(grep -v "^#" ${MODULE_CONF} |sed 's/=.*//')
        do
            del_configuration $i
        done
    fi
}

cleanup()
{
    #clean dir structure in case of script failure
    echo "cleanup..."

    cleanup_configuration
    [ ! -z "$JAR_NAME" ] && rm -f $argv__KEYCLOAK/providers/$JAR_NAME

    echo "done"
}

# Following function is inspired from https://stackoverflow.com/questions/4023830/how-to-compare-two-strings-in-dot-separated-version-format-in-bash
# We just added processing for -SNAPSHOT
vercomp()
{
    # select VERSION1 (more recent) if both version contains -SNAPSHOT (=> -90 / -99)
    VERSION1=$(echo $1 |sed 's/-SNAPSHOT/.-90/')
    VERSION2=$(echo $2 |sed 's/-SNAPSHOT/.-99/')

    if [[ $VERSION1 == $VERSION2 ]]
    then
        echo 0
        return
    fi

    local IFS=.
    local i ver1=($VERSION1) ver2=($VERSION2)
    # fill empty fields in ver1 with zeros
    for ((i=${#ver1[@]}; i<${#ver2[@]}; i++))
    do
        ver1[i]=0
    done
    for ((i=0; i<${#ver1[@]}; i++))
    do
        if [[ -z ${ver2[i]} ]]
        then
            # fill empty fields in ver2 with zeros
            ver2[i]=0
        fi
        if ((10#${ver1[i]} > 10#${ver2[i]}))
        then
            echo 1
            return
        fi
        if ((10#${ver1[i]} < 10#${ver2[i]}))
        then
            echo 2
            return
        fi
    done
    echo 0
    return
}

getDependencyVersion()
{
    # remove rootname and extension
    FILENAME=$(echo $1 |sed "s@.*$2-@@" |sed "s/\.jar//")
    [ -z "$FILENAME" ] && echo "0.0.0" || echo $FILENAME
}

copy_dependency()
{
    FILEPATH=$1
    TARGET=$2

    FILENAME=$(basename ${FILEPATH})
    ROOTNAME=${FILENAME%-[0-9]*}

    FOUND_FILES=$(find ${TARGET} -regex ".*/${ROOTNAME}.*\.jar")
    if [ ! -z "${FOUND_FILES}" ]; then
        for LOCALFILE in ${FOUND_FILES}
        do
            LOCALNAME=$(basename ${LOCALFILE})
            if [[ ${FILENAME} == ${LOCALNAME} ]]; then
                echo "[SKIP] ${FILENAME} already exists"
            else
                VERSION1=$(getDependencyVersion ${FILENAME} ${ROOTNAME})
                VERSION2=$(getDependencyVersion ${LOCALFILE} ${ROOTNAME})
                COMPARE=$(vercomp ${VERSION1} ${VERSION2})
                case $COMPARE in
                    1)
                        echo "[DELE] ${LOCALNAME}" && rm ${LOCALFILE}
                        echo "[COPY] ${FILENAME}" && cp ${FILEPATH} ${TARGET}
                        ;;
                    2)
                        echo "[KEEP] ${LOCALNAME} is more recent"
                        echo "[SKIP] ${FILENAME} too old version"
                        FILEPATH=${LOCALFILE}
                        FILENAME=${LOCALNAME}
                        ;;
                esac
            fi
        done
    else
        echo "[COPY] ${FILENAME} does not exist yet"
        cp ${FILEPATH} ${TARGET}
    fi
}

Main__interruptHandler()
{
    # @description signal handler for SIGINT
    echo "$0: SIGINT caught"
    exit
}
Main__terminationHandler()
{
    # @description signal handler for SIGTERM
    echo "$0: SIGTERM caught"
    exit
}
Main__exitHandler()
{
    cleanup
    if [[ "$EXCEPTION" -ne 0 ]] ; then
        echo "$0: error : ${EXCEPTION_MSG}"
    fi
    exit
}

trap Main__interruptHandler INT
trap Main__terminationHandler TERM
trap Main__exitHandler ERR

Main__main()
{
    # init script temporals
    init_exceptions
    init "$@"
    if [[ "$argv__UNINSTALL" -eq 1 ]]; then
        cleanup
        exit 0
    fi
    # install module
    cp $JAR_PATH $argv__KEYCLOAK/providers/
    for DEPENDENCY in $(dirname $JAR_PATH)/libs/*.jar
    do
        copy_dependency ${DEPENDENCY} $argv__KEYCLOAK/providers/
    done

    # configure module
    cleanup_configuration
    if [ -f "${MODULE_CONF}" ]; then
        grep -v "^#" ${MODULE_CONF} |sed '/^[[:space:]]*$/d' |sed "s!PARAM_TARGET_URI!${argv__TARGET}!g" >> ${CONF_FILE}
    fi

    # $argv__KEYCLOAK/bin/$KC_EXE build

    exit 0
}

Main__main "$@"
