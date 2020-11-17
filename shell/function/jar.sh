#!/bin/bash

# Maven 仓库
MavenRespository='E:/Repository'

jarShell() {
	case $1 in
		'delete')
			ylJarDelete $2;
		;;
	esac
}

ylJarDelete() {

	if [ ! -n "$1" ]; then

		echo "参数为空，直接删除 yunlu 和 yl 下的 全部 jar 包";

		rm -rf $MavenRespository/com/yl/$1/**;
		echo "删除 $MavenRespository/yunlu 下的 $1 jar 成功";

		rm -rf $MavenRespository/com/yunlu/$1/**;
		echo "删除 $MavenRespository/yl 下的 $1 jar 成功";
		return;
	fi

	case $1 in
		'yl')
			rm -rf $MavenRespository/com/yl/**;
			echo "删除 $MavenRespository/yl 下的 jar 成功"
		;;
		'yunlu')
			rm -rf $MavenRespository/com/yunlu/**;
			echo "删除 $MavenRespository/yunlu 下的 jar 成功"
		;;
		*)
			rm -rf $MavenRespository/com/yl/$1/**;
			echo "删除 $MavenRespository/yunlu 下的 $1 jar 成功";

			rm -rf $MavenRespository/com/yunlu/$1/**;
			echo "删除 $MavenRespository/yl 下的 $1 jar 成功";
		;;
	esac
}