#!/bin/bash
# ********************************************************************
# Ericsson Radio Systems AB									 SCRIPT
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
# Name	: streamAdapterMonitor.sh
# Date	: 12/12/2013
# Purpose : Script that monitor the StreamAdapter
# ********************************************************************
#
#	 Command Section
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
#	   $1 - Error message from part of program (Not always used)
# Return Values:
#	   none
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




function get_absolute_path() 
{
	local _dir_=`$DIRNAME $0`
	SCRIPTHOME=`cd $_dir_ 2>/dev/null && pwd || $ECHO $_dir_`
}


function checkStatus() {
	STRING_TO_CHECK="JVM_not"
	while true; do
		JVMS=`${STREAM_ADAPTER} -m -a status -r |$GREP -vw grep|$GREP ${STRING_TO_CHECK}|$NAWK -F ":" '{print $2}'`
		if [ "${JVMS}" ]; then
			JVMS=$(echo "${JVMS}"| sed -e "s/ $//g")
			#echo "${STREAM_ADAPTER_lOCAL} -a restart -j \"${JVMS}\""
			${STREAM_ADAPTER_lOCAL} -a start -m  -j "${JVMS}" >> /dev/null 2>&1
		fi
		sleep $SLEEP_TIME
	done
}



# ********************************************************************
#
#   Main body of program
#
# ********************************************************************
#


TIME_START=`date '+%Y-%b-%d_%H.%M.%S'`
get_absolute_path
# Default user
DEFAULT_USER=dcuser
check_id $DEFAULT_USER
SLEEP_TIME=180



APPLICATION_PATH=`$DIRNAME $SCRIPTHOME`
if [ ! -d "$APPLICATION_PATH" ]; then
	_err_msg_="Directory APPLICATION_PATH ${APPLICATION_PATH} is empty"
	abort_script "$_err_msg_" 
  
fi


STREAM_ADAPTER=$SCRIPTHOME/streamAdapter.sh
if [ ! -x ${STREAM_ADAPTER} ]; then
	_err_msg_="No script ${STREAM_ADAPTER}"
	abort_script "$_err_msg_" 
fi

STREAM_ADAPTER_lOCAL=$SCRIPTHOME/streamAdapter_local.sh
if [ ! -x ${STREAM_ADAPTER_lOCAL} ]; then
	_err_msg_="No script ${STREAM_ADAPTER_lOCAL}"
	abort_script "$_err_msg_" 
fi



checkStatus

