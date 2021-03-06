#!/usr/bin/env bash
#git log --name-status --color=always --pretty=oneline --reverse -n 10
case $1 in '')
	git --no-pager log --pretty="format:%Cred%h%Creset (%cr) %Cblue%s%Creset %+b" --name-status 
;;

*)
	git --no-pager log --pretty="format:%Cred%h%Creset (%cr) %Cblue%s%Creset %+b" --name-status -n $1 
esac

