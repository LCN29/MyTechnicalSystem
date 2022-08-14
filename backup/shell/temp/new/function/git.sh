#!/bin/bash


gitShell() {
	case "$1" in

		'test')
			# 获取 git name
        	gitName=`sed '/^gitName=/!d;s/.*=//' $configPath`;
			echo "测试 $gitName"
		;;

		'name')
			# 设置 Git 本地仓库的用户名
			echo "开始设置 Git 本地仓库的用户名....";
			name='';
			if  [ ! -n "$2" ] ;then
				name=$defaultName;
			else
				name=$2;
			fi

			git config user.name $name;
			echo "Git 本地仓库用户名为 $name 设置成功"
		;;

		'push')
			git add .;
			git commit -m $2;
			git push;
		;;

		'create-branch')

			git checkout master;

			git pull;

			git checkout -b $2;

			git push --set-upstream origin $2;
		;;

		'delete-branch')
			if  [ -n "$2" ] ;then
				# 只删除自己的分支
				if [[ $2 =~ "can" ]]; then

					echo "开始删除分支 $2 ....";
					git checkout master;
					git pull;
					# 删除本地分支
					git branch -D $2;
					# 删除远程分支
					git push origin --delete $2;
					git pull; 
			        echo "删除分支 $2 成功";

				else 
          			echo "需要删除的分支不包含 can 关键字";    
				fi
			else 
		        echo "没有输入要删除的分支名, 不做任何处理";
		    fi
		;;
		'merge-revert')
			git reset --hard HEAD~;
		;;
		'stash-save')
			if  [ ! -n "$2" ] ;then
				git stash;
			else
				git stash save $2;
			fi
		;;
		*)
			echo "unknow command";
		;;
	esac;  
}