#!/bin/bash
# ********************************************************************
# Ericsson Radio Systems AB                                     SCRIPT
# ********************************************************************
#
#
# (c) Ericsson Radio Systems AB 2014 - All rights reserved.
#
# The copyright to the computer program(s) herein is the property
# of Ericsson Radio Systems AB, Sweden. The programs may be used 
# and/or copied only with the written permission from Ericsson Radio 
# Systems AB or in accordance with the terms and conditions stipulated 
# in the agreement/contract under which the program(s) have been 
# supplied.
#
# ********************************************************************
# Name    : create_stream_adapter_ini.sh
# Date    : 05/02/2014
# Revision: A.2
# Purpose : Script to take in details of TOR Streaming servers 
# 			then create a stream_adapter.ini file on the NAS
#			 
#
# ********************************************************************
#
#   Command Section
#
# ********************************************************************
AWK=/usr/bin/awk
BASENAME=/usr/bin/basename
BC=/usr/bin/bc
CAT=/usr/bin/cat
CHMOD=/usr/bin/chmod
CLEAR=/usr/bin/clear
CMP=/usr/bin/cmp
CP=/usr/bin/cp
CUT=/usr/bin/cut
DATE=/usr/bin/date
DF=/usr/bin/df
DIRNAME=/usr/bin/dirname
DOMAINNAME=/usr/bin/domainname
DTCONFIG=/usr/dt/bin/dtconfig
ECHO=/usr/bin/echo
EGREP=/usr/bin/egrep
EJECT=/usr/bin/eject
ENV=/usr/bin/env
EXPR=/usr/bin/expr
FORMAT=/usr/sbin/format
FSTYP=/usr/sbin/fstyp
FUSER=/usr/sbin/fuser
GEGREP=/usr/sfw/bin/gegrep
GETENT=/usr/bin/getent
GETTEXT=/usr/bin/gettext
GREP=/usr/bin/grep
GTAR=/usr/sfw/bin/gtar
GZCAT=/usr/bin/gzcat
HEAD=/usr/bin/head
HOSTID=/usr/bin/hostid
HOSTNAME=/usr/bin/hostname
ID=/usr/bin/id
INIT=/usr/sbin/init
LS=/usr/bin/ls
MKDIR=/usr/bin/mkdir
MORE=/usr/bin/more
MV=/usr/bin/mv
NAWK=/usr/bin/nawk
PGREP=/usr/bin/pgrep
PING=/usr/sbin/ping
PKGADD=/usr/sbin/pkgadd
PKGINFO=/usr/bin/pkginfo
PRTCONF=/usr/sbin/prtconf
PRTVTOC=/usr/sbin/prtvtoc
PSRINFO=/usr/sbin/psrinfo
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
VOLD=/usr/sbin/vold
WC=/usr/bin/wc
ZFS=/usr/sbin/zfs
ZPOOL=/usr/sbin/zpool

# ********************************************************************
#
#       Configuration Section
#
# ********************************************************************


STREAM_ADAPTER_INI=StreamAdapter.ini

# Template StreamApdater.ini file
STREAM_ADAPTER=/

# Directory on the root filesystem
ENIQ_ROOT_DIR=/eniq

DEFAULT_USER=root

# ********************************************************************
#
#       Pre-execution Operations
#
# ********************************************************************


# ********************************************************************
#
#   Functions
#
# ********************************************************************

### Function: abort_script ###
#
#   This will is called if the script is aborted thru an error
#   signal sent by the kernel such as CTRL-C or if a serious
#   error is encountered during runtime
#
# Arguments:
#       $1 - Error message from part of program (Not always used)
# Return Values:
#       none
abort_script()
{
_err_time_=`$DATE '+%Y-%b-%d_%H.%M.%S'`

if [ "$1" ]; then
    _err_msg_="${_err_time_} - $1"
else
    _err_msg_="${_err_time_} - ERROR : Script aborted.......\n"
fi

if [ "${LOGFILE}" ]; then
    $ECHO "\nERROR : $_err_msg_\n" | $TEE -a ${LOGFILE}
else
    $ECHO "\nERROR : $_err_msg_\n"
fi

cd $SCRIPTHOME
$RM -rf ${TEM_DIR}
exit 1

}

### Function: chk_create_logfile ###
#
# Check/Create Logfile
#
# Arguments:
#   none
# Return Values:
#   none
chk_create_logfile()
{
$MKDIR -p `$DIRNAME ${LOGFILE}`
if [ $? -ne 0 ]; then
    _err_msg_="Could not create directory `$DIRNAME ${LOGFILE}`"
     abort_script "$_err_msg_"
fi  

$TOUCH -a ${LOGFILE}
if [ $? -ne 0 ]; then
    _err_msg_="Could not write to file ${LOGFILE}"
    abort_script "$_err_msg_"
fi  

}
### Function: create_stream_med_ini ###
#
# Create the stream_adapter.ini file 
# Arguments:
#   none
# Return Values:
#   none
create_stream_adapter_ini()
{

while :; do
	for (( tor_num = 1 ; tor_num <= ${num_tor_server}; tor_num++)) do
	
		export tor_num
		get_streaming_ip
		DATA_TYPE=CTRS
		export ${DATA_TYPE}
		populate_ctum_ctrs_blocks
		DATA_TYPE=CTUM
		export ${DATA_TYPE}
		populate_ctum_ctrs_blocks
		
	done
	
            
	${ECHO} "Are the above values right? [Yy/Nn]"
	            
	read ANSWER 

	if [ ! "${ANSWER}" ]; then
		continue
	fi	
	
	 if [[ "${ANSWER}" == "Y" || "${ANSWER}" == "y" ]]
			then
			populate_general_block
			break 
    else
		unset tor_num
		break	
	fi
done

}

### Function: get_absolute_path ###
#
# Determine absolute path to software
#
# Arguments:
#   none
# Return Values:
#   none
get_absolute_path() 
{
_dir_=`$DIRNAME $0`
SCRIPTHOME=`cd $_dir_ 2>/dev/null && pwd || $ECHO $_dir_`
}
### Function: get_ctr_jvm ###
#
# Read the number of CTR JVMS 
#
# Arguments:	
# Return Values:
#	none
get_num_tor_servers()
{
$CLEAR 

while :; do
    $ECHO "\n\nEnter the number of TOR PM Streaming servers"
    read _user_input_
	
	    # Did user enter anything
    if [ ! "${_user_input_}" ]; then
		continue
    fi
	
	if [[ ! $_user_input_ = *[[:digit:]]* ]]; then
		${ECHO}  "$_user_input_ is not a number"	
	else 
		break
	fi
done

num_tor_server=${_user_input_}

export num_tor_server
}
get_stream_adpater_details()
{
local _default_stream_adapt_=ec_st

while :; do
	
    $CLEAR
	unset STREAMADAPT_SERVER
	
	SERVER_NAMES="/eniq/sw/conf/service_names"
	if [ ! -s ${SERVER_NAMES} ]; then
		_err_msg_="File ${SERVER_NAMES} is empty"
		abort_script "$_err_msg_" 
	fi
	
	STREAMADAPT_SERVER=$($CAT ${SERVER_NAMES} | $GEGREP -v '^[[:blank:]]*#' | $GEGREP "${_default_stream_adapt_}" | $NAWK -F "::" {'print $3'})
	if [ $? -ne 0 ]; then
		_err_msg_="No Streaming services detected}"
		abort_script "$_err_msg_"
	fi 
	
	
#	if [ ! "${STREAMADAPT_SERVER}" ]; then
#		_err_msg_="No IP for ${EC_2}"
#		  abort_script "$_err_msg_" 
#	fi
    
	$ECHO "\nThe following Streaming Services have been detected  "
	
	$ECHO ${STREAMADAPT_SERVER}

	if [ "${STREAMADAPT_SERVER}" ]; then
	$ECHO "\nFor default Streaming Service(s) listed above Hit Enter , \n Or Enter Streaming service name"
	fi
	
	read _user_input_

	# If the User hit nothing No action required
	if [ ! "${_user_input_}" ]; then
			if [ "${_default_stream_adapt_}" ]; then
				STREAMADAPTER_SERVERS_DETAIlS=${STREAMADAPT_SERVER}
				break
			fi
	continue

	fi
	
	STREAMADAPTER_SERVERS_DETAIlS=${_user_input_}
	
#	STREAMADAPTER_SERVER_IP=$($CAT ${SERVER_NAMES} | $GEGREP -v '^[[:blank:]]*#' | $GEGREP -w "${_user_input_}" | $NAWK -F "::" {'print $1'} | $HEAD -1)
#	if [ ! "${STREAMADAPTER_SERVER_IP}" ]; then
#		_err_msg_="NO IP for ${STREAMADAPTER_SERVER}"
#		 abort_script "$_err_msg_" 
#	fi
	
	break
	
done  

export STREAMADAPTER_SERVERS_DETAIlS


}

### Function: get_streaming_ip ###
#
# Read the Streaming IP Addresses 
#
# Arguments:	
# Return Values:
#	none
get_streaming_ip()
{
local _user_input_ _ip_stream_ 

$CLEAR 

while :; do
    $ECHO "\n\nEnter IP address of TOR ${tor_num} PM Streaming Server"
    read _user_input_

    # Did user enter anything
    if [ ! "${_user_input_}" ]; then
		continue
    fi
	
    validate_ip ${_user_input_}
    if [ $? -ne 0 ]; then
        continue
    fi

    break
done
_ip_stream_=${_user_input_}

# Store the values
STREAMING_IP="${_ip_stream_}"

export STREAMING_IP

}
### Function: populate_ctum_ctrs_blocks ###
#
# Read the CTUM PORT / CTRS PORTS
#
# Arguments:	
# Return Values:
#	none
populate_ctum_ctrs_blocks()
{
local _user_input_ _stream_port_ 
SA_ARRAY=(${STREAMADAPTER_SERVERS_DETAIlS})
SA_NUM=${#SA_ARRAY[@]}

TMP_INPUT_CHECK=0
	
$CLEAR 
while :; do
	$ECHO "\n\nEnter the number of ${DATA_TYPE} PORTS for TOR PM Streaming Server ${STREAMING_IP} "
	read _user_input_

	# Did user enter anything
	if [ ! "${_user_input_}" ]; then
		continue
	fi
	
	if [[ ! $_user_input_ = *[[:digit:]]* ]]; then
		${ECHO}  "$_user_input_ is not a number"
	else
		break
	fi
done

NUM_PORTS=${_user_input_}
  
while [[ "${TMP_INPUT_CHECK}" == 0 ]]
    do        

	TMP_PORT_NUMBER_CHECK=0        
	while [[ "${TMP_PORT_NUMBER_CHECK}" == 0 ]]
	do                   

		#clear the array
		unset PORTS
		
		#clear the read variable
		unset NUMBERS
		
		#clear the wrong port check bit
		unset WRONG_PORT
		
		if [[ "${NUM_PORTS}" == 0 ]]; then
			break
		fi
		
		${ECHO} "Enter the ${DATA_TYPE} ports in the following format [n,n,n-n,n...n]:"
		
		read NUMBERS
		
		# Did user enter anything
		if [ ! "${NUMBERS}" ]; then
			continue
		fi
		   
	  
		if [[ ${NUMBERS} == "" ]]; then
		${ECHO} "Invalid port number ${NUMBERS}, Please try again"
		WRONG_PORT=1
		fi

		for NUMBER in `$ECHO ${NUMBERS} | $SED -e 's| ||g' -e 's|,| |g'`;do
			$ECHO ${NUMBER} | $EGREP '-' >> /dev/null 2>&1
			
			if [ $? -eq 0 ]; then
				START=`${ECHO} ${NUMBER} | $NAWK -F\- '{print $1}'`
				
				END=`${ECHO} ${NUMBER} | $NAWK -F\- '{print $2}'`

				START_CHK=`${ECHO} "${START}" | ${AWK} '$0 ~/[^0-9]/ { print "NOT_NUMBER" }'`
				
				END_CHK=`${ECHO} "${END}" | ${AWK} '$0 ~/[^0-9]/ { print "NOT_NUMBER" }'`

				if [[ "${START}" == "" || "$START_CHK" == "NOT_NUMBER" ]]; then
					${ECHO} "Invalid port number ${START}, Please try again"
					WRONG_PORT=1
					break
				fi  

				if [[ "${END}" == "" || "$END_CHK" == "NOT_NUMBER" ]]; then
					${ECHO} "Invalid port number ${END}, Please try again"
					WRONG_PORT=1
					break
				fi

				if [[ "${END}" -lt "${START}" ]]; then
					${ECHO} "Invalid port number ${END}, Please try again"
						WRONG_PORT=1
						break
				fi 
				
				for (( tmp=${START};tmp<=${END};tmp++))
				do
									
					#unset the duplicate check bit
					unset ISDUP
					
					for (( i=0; i<${#PORTS[@]}; i++))
					do
						if [[ ${tmp} -eq ${PORTS[$i]} ]]; then
							${ECHO} "Duplicate Port number ${tmp}, Ignore"
							ISDUP=1
						fi						
					done
				
					if [[ ${ISDUP} -ne 1 ]];then
					
						PORTS[${#PORTS[@]}]=${tmp}
					fi
					
				done
				
			else
				
				NUM_CHK=`${ECHO} "${NUMBER}" | ${AWK} '$0 ~/[^0-9]/ { print "NOT_NUMBER" }'`
				if [[ "$NUM_CHK" == "NOT_NUMBER" ]]; then
					${ECHO} "Invalid port number ${NUMBERS}, Please try again"
					WRONG_PORT=1
					break
				fi
				
				#unset the duplicate check bit
				unset ISDUP
				
				for (( i=0; i<${#PORTS[@]}; i++))
				do
					if [[ ${NUMBER} -eq ${PORTS[$i]} ]]; then
					${ECHO} "Duplicate Port number $NUMBER, Ignore"
					ISDUP=1
					fi						
				done
				
				if [[ ${ISDUP} -ne 1 ]];then
				
					PORTS[${#PORTS[@]}]=${NUMBER}
				fi
				
			fi
		done
		
		if [[ ${#PORTS[@]} -gt ${NUM_PORTS} ]]; then
			${ECHO} "More than ${NUM_PORTS} port number are provided, Please try again and limit the port numbers"
			continue
		fi
		
		if [[ "${WRONG_PORT}" != "1" ]]; then
		   TMP_PORT_NUMBER_CHECK=1
		fi
		
	done
	
	# Divide Port out evenly to different Streaming Server
	unset even_port_num
	even_port_num=$((${#PORTS[@]} / ${SA_NUM}))
	
	for (( i=0;i<${#PORTS[@]};i++))
		do   

			$RM -rf ${TEM_DIR}/iniblock >> /dev/null 2>&1
			$RM -rf ${TEM_DIR}/new_stream_adapter_ini >> /dev/null 2>&1				
 			
			unset num
			num=`$ECHO $i | ${SED} 's/PORT//'`
			
			unset num_port
			num_port=$((${num}+1))

			if [[ ${NUM_PORTS} == 1 && ${num_tor_server} == 2 ]];then
				if [[ ${tor_num} == 1 ]];then
						Stream_Server=${SA_ARRAY[0]}
				else 
						Stream_Server=${SA_ARRAY[1]}
				fi
			elif [ ${SA_NUM} == 1 ];then

				Stream_Server=${SA_ARRAY[0]}
				
			elif [ ${SA_NUM} == 2 ];then

				if [ ${DATA_TYPE} == CTUM ];then
					if [ ${num_port} -le ${even_port_num}  ];then
						Stream_Server=${SA_ARRAY[0]}
					else
						Stream_Server=${SA_ARRAY[1]}
					fi				
				elif [ ${DATA_TYPE} == CTRS ];then
					if [ ${num_port} -le ${even_port_num} ];then
						Stream_Server=${SA_ARRAY[0]}
					else
						Stream_Server=${SA_ARRAY[1]}
					fi
				fi
			fi
			
			# Update the details file for adding to ini file later
			$ECHO "[Stream${DATA_TYPE}_${num_port}_TOR_${tor_num}]" > ${TEM_DIR}/iniblock
			
			# Update the details file for adding to ini file later
			$ECHO "inputIP=${STREAMING_IP}" >> ${TEM_DIR}/iniblock

			# Update the details file for adding to ini file later
			$ECHO "inputPort=${PORTS[${i}]}" >> ${TEM_DIR}/iniblock
			
			$ECHO "DataType=${DATA_TYPE}" >> ${TEM_DIR}/iniblock
			
			$ECHO "ec=${Stream_Server}" >> ${TEM_DIR}/iniblock
			
			$ECHO "StreamAdapter_Server=${Stream_Server}" >> ${TEM_DIR}/iniblock
			
							
			$INIADD -g STREAM_ADPATER -p Stream${DATA_TYPE}_${num_port}_TOR_${tor_num} -i ${TEMP_FILE} -d ${TEM_DIR}/iniblock -o ${TEM_DIR}/new_stream_adapter_ini
			if [ $? -ne 0 ]; then 
				_err_msg_="Failed to update ${TEMP_FILE}\n"
				abort_script "$_err_msg_"
				fi

			$CP ${TEM_DIR}/new_stream_adapter_ini ${TEMP_FILE}
			if [ $? -ne 0 ]; then 
				_err_msg_="Could not copy ${TEM_DIR}/new_stream_adapter_ini to ${TEMP_FILE}"
				abort_script "$_err_msg_"
			fi
		done
		
		TMP_INPUT_CHECK=1  
        
done 

}

### Function: populate_general_block ###
#
# Create the general block
#
# Arguments:
#   none
# Return Values:
#   none
populate_general_block()
{
local ctum_ports=`${EGREP} StreamCTUM ${TEMP_FILE} | tr -d '[]'`
local ctr_ports=`${EGREP} StreamCTRS ${TEMP_FILE} | tr -d '[]'`
local ctr_array=$ctr_ports
local ctum_array=$ctum_ports
PORT_COUNT=0

$RM -rf ${TEM_DIR}/iniblock >> /dev/null 2>&1


APPLICATION_PATH=`$DIRNAME $SCRIPTHOME`
if [ ! -d "$APPLICATION_PATH" ]; then
    _err_msg_="Directory APPLICATION_PATH ${APPLICATION_PATH} is empty"
    abort_script "$_err_msg_" 
  
fi

$ECHO "[GENERAL]" >> ${TEM_DIR}/iniblock

for j in ${ctr_array[@]}; do
     PORT_COUNT=$((PORT_COUNT+1))
     $ECHO "JVM${PORT_COUNT}="${j}"" >> ${TEM_DIR}/iniblock
done

for j in ${ctum_array[@]}; do
     PORT_COUNT=$((PORT_COUNT+1))
     $ECHO "JVM${PORT_COUNT}="${j}"" >> ${TEM_DIR}/iniblock
done

$CAT ${TEMP_FILE}  >> ${TEM_DIR}/iniblock
if [ $? -ne 0 ]; then
    _err_msg_="Could not update ${TEM_DIR}/iniblock "
    abort_script "$_err_msg_"
fi


if [ -s ${APPLICATION_PATH}/etc/${STREAM_ADAPTER_INI} ]; then
	$CP ${APPLICATION_PATH}/etc/${STREAM_ADAPTER_INI} ${APPLICATION_PATH}/etc/${STREAM_ADAPTER_INI}.orig
	if [ $? -ne 0 ]; then
		_err_msg_="Could not copy backup StreamApdater.ini "
		abort_script "$_err_msg_"
	fi
	$RM -rf ${APPLICATION_PATH}/etc/${STREAM_ADAPTER_INI}
fi

$CP ${TEM_DIR}/iniblock ${APPLICATION_PATH}/etc/${STREAM_ADAPTER_INI}
if [ $? -ne 0 ]; then
    _err_msg_="Could not copy ${APPLICATION_PATH}${STREAM_ADAPTER_INI} restoring orginal ${STREAM_ADAPTER_INI} "
	$CP ${APPLICATION_PATH}/etc/${STREAM_ADAPTER_INI}.orig ${APPLICATION_PATH}/etc/${STREAM_ADAPTER_INI}
    abort_script "$_err_msg_"
fi

if [ -s ${APPLICATION_PATH}/etc/${STREAM_ADAPTER_INI}.orig ]; then
	$RM -rf ${APPLICATION_PATH}/etc/${STREAM_ADAPTER_INI}.orig
fi
}
### Function: setup_env ###
#
# Setup up path environment etc
#
# Arguments:
#   none
# Return Values:
#   none
setup_env()
{
ENIQ_BASE_DIR=${ENIQ_ROOT_DIR}

ENIQ_ADMIN_DIR=${ENIQ_BASE_DIR}/admin

ENIQ_CONF_DIR=${ENIQ_BASE_DIR}/installation/config

ENIQ_INSTALL_DIR=${ENIQ_ROOT_DIR}/installation/core_install

ENIQ_SW_CONF_DIR=${ENIQ_BASE_DIR}/sw/conf

# Set up Variable to hold network path to SW
if [ ! -s ${ENIQ_INSTALL_DIR}/lib/iniadd.pl ]; then
    _err_msg_="Cannot locate ${ENIQ_INSTALL_DIR}/../lib/iniadd"
    abort_script "$_err_msg_" 
else
	INIADD=${ENIQ_INSTALL_DIR}/lib/iniadd.pl
fi

if [ ! -s ${ENIQ_INSTALL_DIR}/lib/inidel.pl ]; then
    _err_msg_="Cannot locate ${ENIQ_INSTALL_DIR}/../lib/inidel"
    abort_script "$_err_msg_" 
else
	INIDEL=${ENIQ_INSTALL_DIR}/lib/inidel.pl
fi

}
# ********************************************************************
#
#   Main body of program
#
# ********************************************************************
#

# Determine absolute path to software
get_absolute_path
   
# Source the common functions
if [ -s ${ENIQ_ROOT_DIR}/installation/core_install/lib/common_functions.lib ]; then
    . ${ENIQ_ROOT_DIR}/installation/core_install/lib/common_functions.lib
else
    _err_msg_="File ${ENIQ_ROOT_DIR}/installation/core_install/lib/common_functions.lib not found"
    abort_script "$_err_msg_"
fi

# Setup up path environment etc
setup_env

if [ ! "${LOGFILE}" ]; then
    LOGFILE="/eniq/log/sw_log/mediation_gw/M_E_CTRS/streamAdapter/stream_adapter_setup.log"
fi	
  
# Check/Create Logfile
chk_create_logfile


# Create a temporary Directory
TEM_DIR=/tmp/manage_stream_adapter_setup.$$.$$

$RM -rf ${TEM_DIR}

$MKDIR -p ${TEM_DIR}
if [ $? -ne 0 ]; then
    _err_msg_="Could not create directory ${TEM_DIR}"
    abort_script "$_err_msg_"
fi

$TOUCH ${TEM_DIR}/StreamApdaterTemp.ini
if [ $? -ne 0 ]; then
    _err_msg_="Could not create StreamApdaterTemp "
    abort_script "$_err_msg_"
fi
#Copy the inputted ini file to TEMP
TEMP_FILE=${TEM_DIR}/StreamApdaterTemp.ini

# get Stream Adapter server details
get_stream_adpater_details

#Read the number of TOR servers
get_num_tor_servers

# Create StreamAdpater.ini
create_stream_adapter_ini

$RM -rf ${TEM_DIR}

exit 0
