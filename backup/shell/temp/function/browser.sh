#!/bin/bash

chromePath="C:/Program Files (x86)/Google/Chrome/Application";

jsonHtmlPath="C:/Users/Administrator/shell/function/html/json.html";

jsonFormatPath="C:/Users/Administrator/shell/function/html/json-format.html";

browserShell() {

	case $1 in
		'open')
			"$chromePath/chrome.exe";
		;;

		'json')
			"$chromePath/chrome.exe" "$jsonHtmlPath";
		;;

		'format')
			"$chromePath/chrome.exe" "$jsonFormatPath";
		;;

		'postman')
			"C:/Users/Administrator/AppData/Local/Postman/Postman.exe";
		;;
	esac
}