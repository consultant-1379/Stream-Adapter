#!/bin/bash

if [ "$2" == "" ]; then
    	echo usage: $0 \<Branch\> \<RState\>
    	exit -1
else
	versionProperties=install/version.properties
	theDate=\#$(date +"%c")
	module=$1
	branch=$2
	workspace=$3
fi

function getProductNumber {
        product=`cat $workspace/build.cfg | grep $module | grep $branch | awk -F " " '{print $3}'`
}


function setRstate {

        #rstate=`cat $workspace/build.cfg | grep $module | grep $branch | awk -F " " '{print $4}'`
	#techpack has no number in rstate. e.g. R6A
	#echo "Building R-State:$rstate"

	revision=`cat $workspace/build.cfg | grep $module | grep " $branch " | awk -F " " '{print $4}'`

        if git tag | grep $module-$revision; then
                rstate=`git tag | grep ${module}-${revision} | tail -1 | sed s/.*-// | perl -nle 'sub nxt{$_=shift;$l=length$_;sprintf"%0${l}d",++$_}print $1.nxt($2) if/^(.*?)(\d+$)/';`
        else
                ammendment_level=01
                rstate=$revision$ammendment_level
        fi
	echo "Building R-State:$rstate"	


}


function nexusDeploy {
	RepoURL=http://eselivm2v214l.lmera.ericsson.se:8081/nexus/content/repositories/releases

	GroupId=com.ericsson.eniq.events
	ArtifactId=$module
	
	echo "****"	
	echo "Deploying the jar /$module-1.0.jar as ${ArtifactId}${rstate}.jar to Nexus...."
        mv target/$module-1.0.jar target/${ArtifactId}.jar
	echo "****"	

  	mvn deploy:deploy-file \
	        	-Durl=${RepoURL} \
		        -DrepositoryId=releases \
		        -Dpackaging=jar \
		        -DgroupId=${GroupId} \
		        -Dversion=${rstate} \
		        -DartifactId=${ArtifactId} \
		        -Dfile=target/${ArtifactId}.jar
 				

}

function uploadArtifactToClearCaseVob {
echo "****"
echo "Moving the artifact to CTRS clearcase vob"
cleartool setview StreamAdapter_build
#cleartool co -nc /vobs/eniq_events/eniq_events_mg/techpack/ctrs/StreamAdapter/
cleartool setview -exec "cleartool co -nc /vobs/eniq_events/eniq_events_mg/techpack/ctrs/StreamAdapter/;" StreamAdapter_build
cleartool setview -exec "cp ${WORKSPACE}/streamAdapter-base/target/StreamAdapt*.tar.gz /vobs/eniq_events/eniq_events_mg/techpack/ctrs/StreamAdapter/StreamAdaptor_$rstate.tar.gz;" StreamAdapter_build
#cleartool mkelem -c "StreamAdaptor_$rstate.tar.gz" /vobs/eniq_events/eniq_events_mg/techpack/ctrs/StreamAdapter/StreamAdaptor_$rstate.tar.gz
cleartool setview -exec "cleartool mkelem -c "StreamAdaptor_$rstate.tar.gz" /vobs/eniq_events/eniq_events_mg/techpack/ctrs/StreamAdapter/StreamAdaptor_$rstate.tar.gz;" StreamAdapter_build
#cleartool ci -nc /vobs/eniq_events/eniq_events_mg/techpack/ctrs/StreamAdapter/StreamAdaptor_$rstate.tar.gz
cleartool setview -exec "cleartool ci -nc /vobs/eniq_events/eniq_events_mg/techpack/ctrs/StreamAdapter/StreamAdaptor_$rstate.tar.gz;" StreamAdapter_build
#cleartool ci -nc /vobs/eniq_events/eniq_events_mg/techpack/ctrs/StreamAdapter/
cleartool setview -exec "cleartool ci -nc /vobs/eniq_events/eniq_events_mg/techpack/ctrs/StreamAdapter/;" StreamAdapter_build
}

getProductNumber
setRstate
git checkout $branch
git pull origin $branch

#add maven command here
mvn clean install -Dmaven.test.skip=true -U -Droot.path=${WORKSPACE}

uploadArtifactToClearCaseVob

git tag $module-$rstate
git pull
git push --tag origin $branch

#nexusDeploy
