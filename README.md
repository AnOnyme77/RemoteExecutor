# RemoteExecutor

The aim of this project is to allow programs and commands execution on multiple computers at a time 

## Needed material and software
* Python installed
* A Unix based system (Linux or mac) : this constraint will be removed in a later version

## Launching
To execute and test this code :
```
sbt run
```

You will be ask to choose between Remote and Local. 

On a first terminal launch Remote and on a second one lauch the Local. 

After that, on the local terminal you can put commands. 
Use the `help` command to see an helper about the possible commands

## Functions

### Balance 
This function allows you to create a distributed and multi threaded consumer/producer mechanism. 

Here is an example of the usage of this function : 
```
balance cluster producer:/Users/anonyme77/Desktop/local.py:test consumer:/Users/anonyme77/Desktop/remote.py:test
```

In this example, the producer (local machine) will run the function test in the python file local.py. 
The outputs of this command will be send to sent to the computers connected to the remote executor system. 
The function named test must require one only argument that is a queue. 
The function have then to put the elements that have to be sent to the remotes in this queue.

On each remote computer, it will execute the function test in the file remote.py.
This function will be given as inputs the outputs of the producer that were sent to this remote computer. 
This funciton must the require a single argument that is one of the output that were sent to this remote by the producer. 
The function will be called iteratively. The argument is then a single element

## Helper
This is the output of the `help`
```
###########################################
--------> Commandes
###########################################
Commande : disconnect -(exemple)-> disconnect name -(explic.)-> disconnect computer identified by 'name'"
Commande : connect -(exemple)-> connect name 127.0.0.1 5150 -(explic.)-> connect computer on address 127.0.0.1 on port 5150 with name 'name'"
Commande : balance -(exemple)-> balance name producer:/path/to/script:function1 consumer:/path/to/script:function2 -(explic.)-> create a system with name 'name' where we execute function1 (this function must have a single argument that is a queue where you have to put your elements) from producer script and give its outputs to function2 (this function must then have a single argument that is the element that is given to the function) on consumer script
Commande : help -(exemple)-> help ls -(explic)-> print this helper
Commande : upload -(exemple)-> upload from to -(explic.)-> upload 'from' local file to 'to' file path on all connected computers"
Commande : python -(exemple)-> python from to -(explic.)-> upload 'from' python script to all computers on 'to' file path and execute the script with python on all connected computers"
Commande : connected -(exemple)-> connected ls -(explic.)-> show all connected computers"
Commande : script -(exemple)-> script /home/script.re -(explic)-> executes the script /home/script.re. This script must contain commands accepted by this command line. It works like an interpreter. Script files can containt script command :)
Commande : exec -(exemple)-> exec cmd -(explic.)-> exec a shell command on all connected computers

###########################################
--------> Commandes dirigées
###########################################
Commande : download -(exemple)-> #name download from to -(explic.)-> download the file 'from' from 'name' computer to local 'to' file path"
```
