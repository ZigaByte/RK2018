#!/bin/sh

for i in {1..10000}
do
   echo "Welcome $i times"

   python3 chatClient.py &
done