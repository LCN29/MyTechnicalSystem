#!/bin/sh

# 配置文件的路径
configPath='./shell.ini';
functionPath='./function'

# 输入指令提示
echo "\n\n";
echo -e "\033[32m当前输入的命令函数为: \033[34m$1\033[0m";


case "$1" in
    'git')
        
        source "$functionPath/git.sh";
		gitShell 'test';
    ;;

    *)
        echo -e "\033[31m未知操作, 请重新输入\033[0m";
        echo "\n\n"
    ;;

esac;