var app = require('express')();
var http = require('http').Server(app);
var io = require('socket.io')(http);

const util = require('util')

var myRooms = [];
var myLeaderboard;


app.get('/', function(req, res){
  res.sendFile(__dirname + '/index.html');
});

http.listen(12345, function(){
  console.log('listening on *:3000');
});

io.on('connection', function(socket){
    console.log('a user connected');
    socket.on('disconnect', function(){
      console.log('user disconnected');
    });
  });

io.on('connection', function(socket){
    socket.on('chat message', function(msg){
        console.log('message: ' + msg);
    });
});


io.on('connection', function(socket){
    socket.on('chat message', function(msg){
      io.emit('chat message', msg);
    });
  });
  


io.on('connection', function(socket) {
  socket.on("createRoom", function(myNewRoom) {
    var roomName = myNewRoom.roomName;
    console.log("create new Room: " + roomName);
    console.log(util.inspect(myNewRoom, {showHidden: false, depth: null}))

    if(myRooms != null) {
      if(myRooms.filter(e => e.name === roomName).length > 0) {
        socket.emit("createRoomReply", false);
      }
      else {
        myRooms.push({name : roomName, createdBy : myNewRoom.createdBy, players : [myNewRoom.createdBy], playerIDs : [socket.id]})
        socket.emit("createRoomReply", true);
      }
    }
    else {
      myRooms.push({name : roomName, createdBy : myNewRoom.createdBy, players : [myNewRoom.createdBy], playerIDs : [socket.id]})
      socket.emit("createRoomReply", true);

    } 
  })
  socket.on("connectRoom", function(myJoinRoom) {
    var roomName = myJoinRoom.roomName;
    console.log("Join Room: " + roomName);
    if(myRooms != null && myRooms.filter(e => e.name === roomName).length === 1) {
      var myRoom = myRooms.filter(e => e.name === roomName)[0];
      console.log(myRoom.players);
      myRooms.filter(e => e.name === roomName)[0].players.push(roomName.playerName)
      console.log(util.inspect(myRoom.playerIDs, {showHidden: false, depth: null}))
      myRooms.filter(e => e.name === roomName)[0].playerIDs.push(socket.id)
      socket.emit("joinRoomInfo", myRoom);
      if(myRoom.quizPack != undefined) {
        console.log("sending quiz");
        console.log(myRoom.quizPack);
        socket.emit("quizPack", myRoom.quizPack);
        socket.emit("quizPack", myRoom.quizPackNext);
        console.log("sending quiz");
        console.log(myRoom.quizPackNext);
        console.log(myRoom.playerIDs)
        io.to(myRoom.playerIDs[0]).emit("playerJoined", "true");
      }
    }
    else {
      socket.emit("joinRoomReply", false);
    }
  })
  socket.on("generatedQuiz", function(quizPack) {
    //console.log(util.inspect(myObject, {showHidden: false, depth: null}))

    console.log("Got new Quiz");
    console.log(util.inspect(quizPack, {showHidden: false, depth: null}))

    var thisRoom = quizPack.roomName;
    console.log(util.inspect(myRooms, {showHidden: false, depth: null}))
    var myRoom = myRooms.filter(e => e.name === thisRoom)[0];
    if(myRooms.filter(e => e.name === thisRoom)[0].quizPack == undefined)
      myRooms.filter(e => e.name === thisRoom)[0].quizPack = quizPack;
    else 
      myRooms.filter(e => e.name === thisRoom)[0].quizPackNext = quizPack
    if(myRoom.playerIDs[1] != undefined)
    console.log("broadcast quiz")
    console.log(quizPack)
    io.to(myRoom.playerIDs[1]).emit("quizPack", quizPack);
    
  })

  socket.on("gameover", function(msg) {
    if(myRooms.filter(e => e.name === msg.roomName)[0].score == undefined)
      myRooms.filter(e => e.name === msg.roomName)[0].score = msg.score;
    else {
      thisRoom = myRooms.filter(e => e.name === msg.roomName)[0];
      otherID = 0;
      if(thisRoom.playerIDs[0] == socket.id)
        otherID = thisRoom.playerIDs[1];
      else
      otherID = thisRoom.playerIDs[0];
      if(myRooms.filter(e => e.name === msg.roomName)[0].score > msg.score){
        socket.emit("result", {result : 1, otherScore : myRooms.filter(e => e.name === msg.roomName)[0].score})
        io.to(otherID).emit("result", {result : 0, otherScore : msg.score})
      }
      else if(myRooms.filter(e => e.name === msg.roomName)[0].score < msg.score) {
        socket.emit("result", {result : 0, otherScore : myRooms.filter(e => e.name === msg.roomName)[0].score})
        io.to(otherID).emit("result", {result : 1, otherScore : msg.score})
      }
      else if (myRooms.filter(e => e.name === msg.roomName)[0].score != undefined && msg.score != undefined){
        socket.emit("result", {result : -1, otherScore : myRooms.filter(e => e.name === msg.roomName)[0].score})
        io.to(otherID).emit("result", {result : -1, otherScore : msg.score})
      }
    }
  })
  
})


//-1 = draw , 0 = win , 1 = lose





/*TODO : 
Handle room leaving
Handle answering
Handle jokers
Handle next round


BIG : Handle answer generation

Handle leaderboard

*/