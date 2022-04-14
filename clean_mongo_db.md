# How to Reset Your Mongo DB:

If the Dispatcher is failing at `checkReset()`, you've probably corrupted your
database by doing an incomplete run...

To Repair follow these steps

1. Delete/Reset the entire Database
    * Run these commands after logging into MongoDB

```
use [database]; 
db.dropDatabase(); 
db.dropAllUsers();
```

2. After running DbLoader in ParaBond
    * Log back into MongoDb and run:

```
db.Bonds.createIndex({id: 1})
db.Portfolio.createIndex({id: 1})
```