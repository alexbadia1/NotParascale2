Setting Up Host C on MongoDB

## TL;DR, Don't Care

On the host with MongoDB installed, open a terminal/command prompt and run:

```
mongod --bing_ip <host machine ip> --port 27012 --dbpath C:\data\db
```

Congrats, you just setup the MongDB Server.

__OPEN ANOTHER__ terminal/command prompt on the same host and run:

```
mongo --port 27017
```

Congrats, you logged into the MongoDB server.

## Virgin Setup

1. Download MongoDB
2. Add MonogDB to PATH
    * this let's you run MongoDB commands in the terminal or command prompt.
3. Start MongoDB Server:
    * `mongod --bing_ip <host machine ip> --port 27012 --dbpath C:\data\db`
        * MongoDB is now running live on your laptop, at <host machine ip>:
          27017!
        * You can literally communicate to it now from a different device.
4. __KEEPING THE PREVIOUS TERMINAL/COMMAND PROMPT OPEN__
    * On the same device, __open a new terminal/command prompt__ and log into
      the live MongoDB you're hosting by typing:
        * `mongo --port 27017`
        * You can now see the current real time state of the database, by typing
          commands like `show collections`.
5. Run ParaBond's DbLoader with the VM arg
   `-Dhost= <ip address of machine with mongo>`
6. once it's done, you can check to see if the Bonds and Porfolios are in Mongo
   by typing `show collections` in the terminal/command prompt that you
   originally logged into MongoDB with.
    * There's probably a way to remote log in to, but that's not needed for this
      project...
