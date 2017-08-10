# RemoteExecutor

The aim of this project is to allow programs and commands execution on multiple computers at a time 

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
