#!/bin/sh

# 支持的配置属性
GitNameKey="GitName"



# 配置文件的路径
configPath='./config.ini';
functionPath='./function'

source ./config.ini;

# 输入指令提示
echo -e "\n\n";
echo -e "\033[32m当前输入的命令函数为: \033[34m$1\033[0m";


case "$1" in
    'git')
        source "$functionPath/git.sh";
	    gitShell $2 $3;
    ;;

    *)
        echo -e "\033[31m未知操作, 请重新输入\033[0m";
        echo -e "\n\n"
    ;;

esac;