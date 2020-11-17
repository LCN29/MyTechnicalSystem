#!/bin/bash

shardingShell() {
	case "$1" in
		'dev')
			cd D:/Sharding-Proxy-4.0.1/Sharding-Proxy-4.0.1-dev/bin/;
			./start.bat ;
		;;

		'test')
			cd D:/Sharding-Proxy-4.0.1/Sharding-Proxy-4.0.1-test/bin/;
			./start.bat 3308;
		;;
	esac;
}

