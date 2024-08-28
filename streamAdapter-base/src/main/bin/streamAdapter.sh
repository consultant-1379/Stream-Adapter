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
# Name    : streamAdapter.sh
# Date    : 08/11/2013
# Purpose : Script that will start/stop/status Stream Adapter
###_VERSION_##
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



function get_absolute_path() 
{
    local _dir_=`$DIRNAME $0`
    SCRIPTHOME=`cd $_dir_ 2>/dev/null && pwd || $ECHO $_dir_`
}


function trap_shutdown_and_exit()
{
local exit_status
if [[ -n "${1}" ]]; then
    exit_status=${1}
else
    exit_status=1
fi
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
REMOTE="YES"
MZ_SERVER_VAR_INI="ec"
SERVER_VAR_INI="StreamAdapter_Server"

check_id $DEFAULT_USER
while :
do
    case $1 in
        -h | --help | -\?)
            show_help
            exit 0
            ;;
        -f )
            INI_FILE=$2
            shift 2
            ;;
        -a )
            ACTION=$2
            shift 2
            ;;
        -r )
            REMOTE="NO"
            shift
            ;;
        -s )
            SERVICE_OPTION=$2
            shift 2
            ;;
        -d )
            DEBUG="YES"
            shift
            ;;
        -m )
            MONITOR="NO"
            shift
            ;;
        *)  # no more options. Stop while loop
            break
            ;;
    esac
done

check_action

if [ "${REMOTE}" == "NO" -a "${SERVICE_OPTION}" != "" ]; then
    _err_msg_="Can't have -r  and -s options together"
    abort_script "$_err_msg_"
fi

APPLICATION_PATH=`$DIRNAME $SCRIPTHOME`
if [ ! -d "$APPLICATION_PATH" ]; then
    _err_msg_="Directory APPLICATION_PATH ${APPLICATION_PATH} is empty"
    abort_script "$_err_msg_" 
  
fi
if [ "${INI_FILE}" == "" ]; then
    INI_FILE=$APPLICATION_PATH/etc/StreamAdapter.ini
fi
if [ ! -s ${INI_FILE} ]; then
    _err_msg_="File ${INI_FILE} is empty"
    abort_script "$_err_msg_" 
fi
STREAM_ADAPTER_LOCAL=$SCRIPTHOME/streamAdapter_local.sh
if [ ! -x ${STREAM_ADAPTER_LOCAL} ]; then
    _err_msg_="No script ${STREAM_ADAPTER_LOCAL}"
    abort_script "$_err_msg_" 
fi

HOSTNAME=`$MYHOSTNAME`
if [ ! "${HOSTNAME}" ]; then
    _err_msg_="Could not get hostname"
    abort_script "$_err_msg_" 
fi

LOCAL_IP=`$GETENT hosts ${HOSTNAME} | nawk '{print $1}' | head -1`
if [ ! "${LOCAL_IP}" ]; then
    _err_msg_="Could not get ipaddress for $HOSTNAME"
    abort_script "$_err_msg_" 
fi

if [ "${DEBUG}" == "YES" ]; then
	DEBUG_OPTION="-d"
fi
if [ "${MONITOR}" == "NO" ]; then
	MONITOR_OPTION="-m"
fi

get_run_server_ips "${SERVICE_OPTION}"

 while IFS=',' read -ra JVMS; do
	locahost_server="NO"
      for i in "${JVMS[@]}"; do
        SERVER_IP=$(echo "$i" | nawk -F ":" '{print $1}')
		SERVER_JVMS=$(echo "$i" | nawk -F ":" '{print $2}')
		if [ "$SERVER_JVMS" != "" ] ; then
			if [ `echo $SERVER_IP | grep -w  ${LOCAL_IP}` ]; then
				${STREAM_ADAPTER_LOCAL} -f ${INI_FILE}  ${DEBUG_OPTION} ${MONITOR_OPTION} -a $ACTION -j "${SERVER_JVMS}"
				if [ $? -ne 0 ]; then
					echo "error" 
				fi
			elif [ "${REMOTE}" == "YES" ]; then
				ssh -q ${SERVER_IP} "${STREAM_ADAPTER_LOCAL}  ${DEBUG_OPTION} ${MONITOR_OPTION} -a ${ACTION} -j \"${SERVER_JVMS}\" 2>/dev/null"
				if [ $? -ne 0 ]; then
					echo "error" 
				fi
			fi
		fi
      done
 done <<< "$IP_JVMS"

#echo "${IP}"
exit 0
