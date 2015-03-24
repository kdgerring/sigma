#!/bin/bash -e

[ -z "$SIGMA_HOME" ] && export SIGMA_HOME="`cd ~ && pwd`/.sigmakee"
SIGMA_SRC="`cd ~ && pwd`/workspace/sigma"

log() {
	case $1 in
		info) echo && echo "$(tput setaf 2)* ${2}$(tput sgr0)" ;;
		warn) echo "$(tput setaf 3)  (!) ${2}$(tput sgr0)" ;;
		fail) echo && echo "$(tput setaf 1)ERROR: ${2}$(tput sgr0)"; exit 1 ;;
		*) echo "  $1" ;;
	esac
}

welcome() {
	log "Welcom to the installation of:"
	log '  _________.___  ________    _____      _____'
	log ' /   _____/|   |/  _____/   /     \    /  _  \'
	log ' \_____  \ |   /   \  ___  /  \ /  \  /  /_\  \'
	log ' /        \|   \    \_\  \/    V    \/    |    \'
	log '/_______  /|___|\______  /\____|__  /\____|__  /'
	log '        \/             \/         \/         \/'
	log "Home: https://sourceforge.net/projects/sigmakee/"
}

check_supported_os() {
	if [ "$(uname)" == "Darwin" ]; then
		PLATFORM="mac"
	elif [ -n "$(lsb_release -d | grep -i ubuntu)" ]; then
		PLATFORM="ubuntu"
	else
		log fail "Your operating system is not supported. Only Ubuntu and Mac OSX are supported."
	fi
}

check_env()
{
	# java cvs maven ant
    case $PLATFORM in
        mac)
			if [ -z "$JDK_HOME" ]; then
				if hash /usr/libexec/java_home 2>/dev/null; then
					JDK_HOME="$(/usr/libexec/java_home)"
				fi
				if [ -z "$JDK_HOME" ]; then
					log fail "No JDK found, please install Java from http://www.oracle.com/technetwork/java/javase/downloads/index.html"
				fi
			fi
			log info "Using existing java installation from: $JDK_HOME"

			if ! hash cvs 2>/dev/null; then
				brew install cvs
			fi
			if ! hash ant 2>/dev/null; then
				brew install ant
			fi
			if ! hash mvn 2>/dev/null; then
				brew install maven
			fi

			;;
		ubuntu)
			log warn "Ubuntu is going to ask your user's password for missed packages check."
			sudo apt-get -y update
			sudo DEBIAN_FRONTEND=noninteractive apt-get install -y cvs ant maven
			;;
	esac
}

change_src_home() {
	log warn "Please provide the full path (without '~') where you want to put sources of SIGMA"
	read -p "  To use default \"$SIGMA_SRC\" just press [ENTER]: " src_home
	[ -z "$src_home" ] || SIGMA_SRC=$src_home
}

download_src() {
	log info "Installing SIGMA to: $SIGMA_SRC"
	mkdir -p $SIGMA_SRC
	cd $SIGMA_SRC
	read -p  "  Provide your SourceForge account name [anonymous]: " ss_user
	if [ -z "$ss_user" ]; then
		ss_user=":pserver:anonymous"
	else
		log warn "To access the code CVS is going to ask your SourceForge password (3 times)."
		ss_user=":ext:${ss_user}"
	fi
	cvs -z3 -q -d "${ss_user}@sigmakee.cvs.sourceforge.net:/cvsroot/sigmakee" co -P sigma
	cvs -z3 -q -d "${ss_user}@sigmakee.cvs.sourceforge.net:/cvsroot/sigmakee" co -P KBs
	cvs -z3 -q -d "${ss_user}@sigmakee.cvs.sourceforge.net:/cvsroot/sigmakee" co -P Vampire
}

sigma_install() {
	log info "Setting SIGMA_HOME to: $SIGMA_HOME"
	cd "${SIGMA_SRC}/sigma"
	ant install
}

sigma_done() {
	log info "SIGMA is ready to use"
	log warn "To run SIGMA in the future you must use following commands:"
	log warn "  » export SIGMA_HOME=${SIGMA_HOME}"
	log warn "  » cd ${SIGMA_SRC}/sigma"
	log warn "  » mvn -DskipTests clean install tomcat7:run"
	echo
	log warn "After it started you can open: http://localhost:9090/sigma/login.html"
	log warn "Default credentials are: admin/admin"
}

sigma_start() {
    read -p "  Do you want me to run SIGMA for you this time? [Y/n]: " sigma_run
	if [[ -z "$sigma_run" || $sigma_run =~ [yY][eE][sS]|[yY] ]]; then
		cd ${SIGMA_SRC}/sigma
		export SIGMA_HOME=${SIGMA_HOME}
		mvn -DskipTests clean install tomcat7:run
	fi
}

welcome
check_supported_os
check_env
change_src_home
download_src
sigma_install
sigma_done
sigma_start
