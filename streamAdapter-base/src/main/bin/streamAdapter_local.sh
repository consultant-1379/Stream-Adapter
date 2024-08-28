#!/bin/bash
# ********************************************************************
# Ericsson Radio Systems AB                                     SCRIPT
# ********************************************************************
#
#
# (c) Ericsson Radio Systems AB 2013 - All rights reserved.
#
# The copyright to the computer program(s) herein is the property
# of Ericsson Radio Systems AB, Sweden. The programs may be used 
# and/or copied only with the written permission from Ericsson Radio 
# Systems AB or in accordance with the terms and conditions stipulated 
# in the agreement/contract under which the program(s) have been 
# supplied.
#
# ********************************************************************
# Name    : streamAdapter_local.sh
# Date    : 11/03/2013
# Purpose : Script that will start/stop/status Stream Adapter JVM on local machine
# ********************************************************************
#
#     Command Section
#
# ********************************************************************
AWK=/usr/bin/awk
BASENAME=/usr/bin/basename
CAT=/usr/bin/cat
CHMOD=/usr/bin/chmod
CHOWN=/usr/bin/chown
CLEAR=/usr/bin/clear
CP=/usr/bin/cp
CUT=/usr/bin/cut
DATE=/usr/bin/date
DIRNAME=/usr/bin/dirname
DOMAINNAME=/usr/bin/domainname
DTCONFIG=/usr/dt/bin/dtconfig
ECHO=/usr/bin/echo
EGREP=/usr/bin/egrep
EJECT=/usr/bin/eject
ENV=/usr/bin/env
EXPR=/usr/bin/expr
FIND=/usr/bin/find
FORMAT=/usr/sbin/format
FUSER=/usr/sbin/fuser
GEGREP=/usr/sfw/bin/gegrep
GETENT=/usr/bin/getent
GETTEXT=/usr/bin/gettext
GREP=/usr/bin/grep
GROUPADD=/usr/sbin/groupadd
GTAR=/usr/sfw/bin/gtar
GZCAT=/usr/bin/gzcat
HEAD=/usr/bin/head
MYHOSTNAME=/usr/bin/hostname
ID=/usr/bin/id
INIT=/usr/sbin/init
KILL=/usr/bin/kill
LS=/usr/bin/ls
LSOF=/usr/local/bin/lsof
MKDIR=/usr/bin/mkdir
MORE=/usr/bin/more
MV=/usr/bin/mv
NAWK=/usr/bin/nawk
PGREP=/usr/bin/pgrep
PING=/usr/sbin/ping
PKGADD=/usr/sbin/pkgadd
PKGINFO=/usr/bin/pkginfo
PRTCONF=/usr/sbin/prtconf
PRINTF=/usr/bin/printf
PS=/usr/bin/ps
PWD=/usr/bin/pwd
RM=/usr/bin/rm
RCP=/usr/bin/rcp
ROLES=/usr/bin/roles
RSH=/usr/bin/rsh
SED=/usr/bin/sed
SLEEP=/usr/bin/sleep
SORT=/usr/bin/sort
SU=/usr/bin/su
SVCADM=/usr/sbin/svcadm
SVCCFG=/usr/sbin/svccfg
SVCS=/usr/bin/svcs
SYNC=/usr/sbin/sync
TAIL=/usr/bin/tail
TEE=/usr/bin/tee
TOUCH=/usr/bin/touch
TPUT=/usr/bin/tput
UADMIN=/usr/sbin/uadmin
UNAME=/usr/bin/uname
USERADD=/usr/sbin/useradd
VOLD=/usr/sbin/vold
WC=/usr/bin/wc
ZFS=/usr/sbin/zfs
ZPOOL=/usr/sbin/zpool
# ********************************************************************
#
#   functions
#
# ********************************************************************
### Function: abort_script ###
#
#   This will is called if the script is aborted thru an error
#   error signal sent by the kernel such as CTRL-C or if a serious
#   error is encountered during runtime
#
# Arguments:
#       $1 - Error message from part of program (Not always used)
# Return Values:
#       none
function abort_script()
{
    _err_time_=`$DATE '+%Y-%b-%d_%H.%M.%S'`

    if [ "$1" ]; then
        _err_msg_="${_err_time_} - $1"
    else
        _err_msg_="${_err_time_} - Script aborted.......\n"    
    fi
    if [ "${LOGFILE}" ]; then
        $ECHO "\nERROR : ${_err_msg_}\n" | $TEE -a ${LOGFILE}
    else
        $ECHO "\nERROR : ${_err_msg_}\n"
    fi

    cd $SCRIPTHOME
    cleanup

    if [ "$2" ]; then
        ${2}
    else
       exit 1
    fi
}


function check_id()
{
_check_id_=`$ID | $AWK -F\( '{print $2}' | $AWK -F\) '{print $1}'`
if [ "$_check_id_" != "$1" ]; then
    _err_msg_="You must be $1 to execute this script."
    abort_script "${_err_msg_}"
fi
}

function cleanup()
{
    $RM -rf ${TMP_FILE_PID}
    $RM -rf ${TMP_JVMS_RUNNING}
	$RM -rf ${TMP_JVMS_NOT_RUNNING}
}




# Init script helper functions
function get_free_jmx_port() {
  local _no_count_=10
  local _count_=1
  local _port_found_=1
  until [ "$_count_" -eq "$_no_count_" ]; do 
    sleep 1
    local output
    output=`$LSOF -i -P | $EGREP ":$JMX_PORT "`
    if [ -z "$output" ]; then
        _port_found_=0
        break
    fi
    (( JMX_PORT+=1 ))
    (( _count_+=1 ))
  done
    if [ $_port_found_ -ne 0 ]; then
        _err_msg_="Can't get a free port, tried $_count_ ports before $JMX_PORT"
        abort_script "$_err_msg_" 
    fi
}

function get_absolute_path() 
{
    local _dir_=`$DIRNAME $0`
    SCRIPTHOME=`cd $_dir_ 2>/dev/null && pwd || $ECHO $_dir_`
}
function printRun() 
{
	echo -n "JVM_${2}:"
    for _jvms_ in `$CAT ${1}`; do
        echo -n "$_jvms_ " 
    done
	echo ""
}

function status() {
    ALL_RUNNING=0
    TMP_JVMS_RUNNING=/var/tmp/streamAdapter_running.$$.txt
	TMP_JVMS_NOT_RUNNING=/var/tmp/streamAdapter_not_running.$$.txt
    if [ -s ${TMP_JVMS_RUNNING} ]; then
        $RM -rf $TMP_JVMS_RUNNING
    fi
    if [ -s ${TMP_JVMS_NOT_RUNNING} ]; then
        $RM -rf $TMP_JVMS_NOT_RUNNING
    fi

	
    #use jps to get the processes running
     $JPS -v | $EGREP $JAVA_MAIN_NAME \
        | $SED 's/.*DLOG_DIR//g' |     \
        $NAWK -F/ '{print $NF }' > $TMP_JVMS_RUNNING
    #check that the process running are of the jvms in ini file
    for _jvms_ in ${JVMS} ; do
        $CAT $TMP_JVMS_RUNNING | $EGREP "^$_jvms_$" >> /dev/null 2>&1
        if [ $? -ne 0 ]; then
			$ECHO $_jvms_ >> $TMP_JVMS_NOT_RUNNING
            ALL_RUNNING=1
		fi
    done
        
	if [ $ALL_RUNNING -eq 0 ]; then
        $ECHO "StreamAdapter is running"
	else
		$ECHO "StreamAdapter not running"
	fi
	if [[ "${DEBUG}" == "YES" || "${SHOW_NOT_RUNNING}" == "YES" ]]; then
	   if [ -s ${TMP_JVMS_RUNNING} ]; then
        printRun ${TMP_JVMS_RUNNING} "is"
       fi
	   if [ -s ${TMP_JVMS_NOT_RUNNING} ]; then
	    printRun ${TMP_JVMS_NOT_RUNNING} "not"
       fi
    fi
    return $ALL_RUNNING
    }

function stop() {
	local _all_=$1
    killMonitor
    $ECHO "Stopping JVM's" | $TEE -a $LOGFILE
	#use jps to get the processes running
    TMP_FILE_PID=/var/tmp/streamAdapter_pids.$$.txt
    if [ -s ${TMP_FILE_PID} ]; then
        $RM -rf $TMP_FILE_PID
    fi
	
	if [[ "${_all_}" == "all" ]]; then
		#use jps to get the processes running
		 $JPS -v | $EGREP $JAVA_MAIN_NAME  \
			| $NAWK  '{print $1}' > $TMP_FILE_PID
	else
		for _jvms_ in ${JVMS} ; do
			$JPS -v | $EGREP $JAVA_MAIN_NAME | $EGREP $_jvms_ | $NAWK  '{print $1}' >>$TMP_FILE_PID
		done
	fi
    #_repeat_ variable is the number of time to check to see if pid died wit 1 sec sleep in between
    #there it will wait 6 secounds if _repeat_ is 6
    for _pid_ in `$CAT ${TMP_FILE_PID}`; do    
        $KILL $_pid_
        local _repeat_=12
        local _count_=0
        local _failed_to_stop_=0
        until [ "$_count_" -eq "$_repeat_" ]; do 
            $PS -p $_pid_  >> /dev/null 2>&1
            if [ $? -eq 0 ]; then
                _failed_to_stop_=1
                (( _count_+=1 ))
                sleep 1
            else
                _failed_to_stop_=0
                break
            fi
        done
        if [ $_failed_to_stop_ -eq 1 ]; then
            $KILL -9 $_pid_
        fi
    done
    status


}
function start() {
    $ECHO "Checking current status" | $TEE -a $LOGFILE
    status  >> /dev/null 2>&1
    if [ $? -ne 1 ]; then
        $ECHO "Running will not restart"
        return 0
    fi
    #
    # Default values (if not set)
    #
    MAXIMUM_HEAP=${MAXIMUM_HEAP:-2g}
    YOUNG_HEAP=${YOUNG_HEAP:-512m}
    INITIAL_HEAP=${MAXIMUM_HEAP}  # set initial Java heap size
    JRE=${APPLICATION_PATH}/jre/bin/java
    if [ ! -s ${JRE} ]; then
        _err_msg_="File ${JRE} is empty"
        abort_script "$_err_msg_" 
    fi
    memory_prop=

    #
    # Heap sizing
    #
    memory_prop+=" -Xmx${MAXIMUM_HEAP}"       # set maximum Java heap size
    memory_prop+=" -Xms${INITIAL_HEAP}"       # set initial Java heap size
    #
    memory_prop+=" -XX:PermSize=64m"
    memory_prop+=" -XX:MaxPermSize=64m"
    memory_prop+=" -XX:InitialCodeCacheSize=8m"
    memory_prop+=" -XX:ReservedCodeCacheSize=48m"
    CLASSPATH=.:$APPLICATION_PATH/etc
    for _name_ in `$LS $APPLICATION_PATH/lib/*.jar`;do
        CLASSPATH=$CLASSPATH:$_name_
    done

    JMX_PORT=20109
    for _jvm_ in ${JVMS} ; do
        #dont't remove the _jvm_ from SECTION_LOG_DIR it's used for status
        SECTION_LOG_DIR="$LOG_DIR/$_jvm_"
        LOG_DIR_ARG="-Dlog4j.configuration=$LOGGER_PATH -DLOG_DIR=$SECTION_LOG_DIR"
        CONFIG_DIR_ARG="-f ${INI_FILE} -i ${_jvm_}"
        #
        # JMX settings, only used if JMX_PORT is set
        #
        get_free_jmx_port
        jmx_prop=
        jmx_prop+=" -Dcom.sun.management.jmxremote"
        jmx_prop+=" -Dcom.sun.management.jmxremote.port=${JMX_PORT}"
        jmx_prop+=" -Dcom.sun.management.jmxremote.authenticate=false"
        jmx_prop+=" -Dcom.sun.management.jmxremote.ssl=false"
        jmx_prop+=" -Djava.rmi.server.hostname=${IP}"
        $ECHO "Starting $_jvm_ with arguments"| $TEE -a $LOGFILE
        $ECHO "$JRE -cp ${CLASSPATH} ${memory_prop} ${jmx_prop} ${LOG_DIR_ARG} ${JAVA_MAIN_CLASS} ${CONFIG_DIR_ARG}" | $TEE -a $LOGFILE
        $JRE -cp ${CLASSPATH} ${memory_prop} ${jmx_prop} ${LOG_DIR_ARG} ${JAVA_MAIN_CLASS} ${CONFIG_DIR_ARG} >> $LOGFILE 2>&1 < /dev/null &
        #increment the JMX_PORT
        (( JMX_PORT+=1 ))
    done
    sleep 2
    status
    if [ $? -ne 0 ]; then
        _err_msg_="Error starting StreamAdapter $_jvms"
        abort_script "$_err_msg_"
    fi 
    startMonitor
}

function restart() {
stop "all"
start
}

function startMonitor() {
    if [ "${MONITOR}" == "YES" ]; then
        ${STREAM_ADAPTER_MONITOR}&
    fi
}

function killMonitor() {
    if [ "${MONITOR}" == "YES" ]; then
        STREAM_ADAPTER_MONITOR_PID=""
        STREAM_ADAPTER_MONITOR_PID=`${PS} -eaf|$GREP -vw grep|$GREP ${STREAM_ADAPTER_MONITOR}|$NAWK '{print $2}'|$SED -e 's/ //g'`
        for i in $STREAM_ADAPTER_MONITOR_PID ; do
             if [ "${STREAM_ADAPTER_MONITOR_PID}" ]; then
                 $KILL -9 ${STREAM_ADAPTER_MONITOR_PID}
             fi
        done
    fi
}


function trap_shutdown_and_exit()
{
local exit_status
if [[ -n "${1}" ]]; then
    exit_status=${1}
else
    exit_status=1
fi
MONITOR="YES"
killMonitor
cleanup
exit ${exit_status}
}
# ********************************************************************
#
#   Main body of program
#
# ********************************************************************
#

trap "trap_shutdown_and_exit 0" QUIT TERM ABRT TSTP SIGHUP SIGINT SIGTERM
TIME_START=`date '+%Y-%b-%d_%H.%M.%S'`
get_absolute_path
# Source the common functions
if [ -s ${SCRIPTHOME}/streamAdapter.lib ]; then
    . ${SCRIPTHOME}/streamAdapter.lib
else
    _err_msg_="File ${SCRIPTHOME}/streamAdapter.lib not found"
    abort_script "$_err_msg_"
fi
# Default user
DEFAULT_USER=dcuser
MONITOR="YES"
check_id $DEFAULT_USER
while :
do
    case $1 in
        -h | --help | -\?)
            show_help
            exit 0
            ;;
        -Xmx=*)
            MAXIMUM_HEAP=${1#*=}
            shift
            ;;
        -Xmn=*)
            YOUNG_HEAP=${1#*=}
            shift
            ;;
        -f )
            INI_FILE=$2
            shift 2
            ;;
        -j )
            JVMS=$2
            shift 2
            ;;
        -a )
            ACTION=$2
            shift 2
            ;;
        -m )
            MONITOR="NO"
            shift
            ;;
        -d )
            DEBUG="YES"
            shift
            ;;

        --) # End of all options
            shift
            break
            ;;
        -*)
            #echo "WARN: Unknown option: $1" >&2
            OTHER_PARAMS+=" ${1}"
            shift
            ;;
        *)  # no more options. Stop while loop
            break
            ;;
    esac
done

if [ ! "${JVMS}" ]; then
    _err_msg_="Could not get JVMS, needs -j option"
    abort_script "$_err_msg_" 
fi
if [ "${MONITOR}" == "NO" ]; then
    SHOW_NOT_RUNNING="YES"
fi

APPLICATION_PATH=`$DIRNAME $SCRIPTHOME`
if [ ! -d "$APPLICATION_PATH" ]; then
    _err_msg_="Directory APPLICATION_PATH ${APPLICATION_PATH} is empty"
    abort_script "$_err_msg_" 
  
fi


LOGGER_PATH=file://$APPLICATION_PATH/etc/logback.xml
if [ ! "${INI_FILE}" ]; then
    INI_FILE=$APPLICATION_PATH/etc/StreamAdapter.ini
fi
if [ ! -s ${INI_FILE} ]; then
    _err_msg_="File ${INI_FILE} is empty"
    abort_script "$_err_msg_" 
fi

LOG_DIR=/eniq/log/sw_log/mediation_gw/M_E_CTRS/streamAdapter
if [ ! -d "$LOG_DIR" ]; then
    $MKDIR -p $LOG_DIR
    if [ $? -ne 0 ]; then
        _err_msg_="Failed to create $LOG_DIR "
        abort_script "$_err_msg_" 
    fi
  
fi

STREAM_ADAPTER_MONITOR=$SCRIPTHOME/streamAdapterMonitor.sh
if [ ! -x ${STREAM_ADAPTER_MONITOR} ]; then
    _err_msg_="No monitoring script ${STREAM_ADAPTER_MONITOR}"
    abort_script "$_err_msg_" 
fi

HOSTNAME=`$MYHOSTNAME`
if [ ! "${HOSTNAME}" ]; then
    _err_msg_="Could not get hostname"
    abort_script "$_err_msg_" 
fi
IP=`$GETENT hosts ${HOSTNAME} | nawk '{print $1}' | head -1`
if [ ! "${IP}" ]; then
    _err_msg_="Could not get ipaddress for $HOSTNAME"
    abort_script "$_err_msg_" 
fi
SCRIPTNAME=`$BASENAME $0`
if [ ! "${SCRIPTNAME}" ]; then
    _err_msg_="Could not get scriptname"
    abort_script "$_err_msg_" 
fi


check_action
if [ `echo $ACTION | egrep "^start|stop|restart$"` ]; then
    LOGFILE_NAME=$ACTION
    LOGFILE_NAME+="_"`$ECHO $SCRIPTNAME | $SED -e 's/\.[^\.]*$//g'`
    LOGFILE_NAME+="_"$TIME_START".log"
    LOGFILE=$LOG_DIR/$LOGFILE_NAME 
    #$ECHO "LOGFILE $LOGFILE"
fi



JPS=/usr/jdk/latest/bin/jps
if [ ! -s ${JPS} ]; then
    _err_msg_="File ${JPS} is empty"
    abort_script "$_err_msg_" 
fi

JAVA_MAIN_CLASS="com.ericsson.streamAdapter.StreamService"
JAVA_MAIN_NAME=$($ECHO $JAVA_MAIN_CLASS | $NAWK -F "." '{print $NF}' )
if [ ! "${JAVA_MAIN_NAME}" ]; then
    _err_msg_="Could not get JAVA_MAIN_NAME"
    abort_script "$_err_msg_" 
fi



${ACTION}

cleanup


exit 0
