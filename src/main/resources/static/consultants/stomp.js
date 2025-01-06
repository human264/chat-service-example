// ----------------------------------------------------
// 1) Declare subscription in the global scope
// ----------------------------------------------------
let subscription = null;

// ----------------------------------------------------
// 2) Create the Stomp client
// ----------------------------------------------------
const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/stomp/chats'
});

// Called when STOMP is connected
stompClient.onConnect = (frame) => {
    setConnected(true);
    showChatrooms(0);
    stompClient.subscribe('/sub/chats/updates', (chatMessage) => {
        toggleNewMessageIcon(JSON.parse(chatMessage.body).id, true);
        updateMemberCount(JSON.parse(chatMessage.body));
    });
    console.log('Connected: ' + frame);
};

function updateMemberCount(chatroom) {
    $('#memberCount_' + chatroom.id).html(chatroom.memberCount);

}

function toggleNewMessageIcon(chatroomId, toggle) {
    if (chatroomId === $("#chatroom-id").val()) {
        return;
    }
    if (toggle) {
        $("#new_" + chatroomId).show();
    } else {
        $("#new_" + chatroomId).hide();
    }
}

// Error handlers
stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
};

stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
};

// Enable/disable UI elements based on connection state
function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    $("#create").prop("disabled", !connected);
}

// STOMP connect/disconnect
function connect() {
    stompClient.activate();
}

function disconnect() {
    stompClient.deactivate();
    setConnected(false);
    console.log("Disconnected");
}

// Send a chat message
function sendMessage() {
    let chatroomId = $("#chatroom-id").val();
    stompClient.publish({
        destination: "/pub/chats/" + chatroomId,
        body: JSON.stringify({
            'message': $("#message").val()
        })
    });
    $("#message").val("");
}

// Fetch and display messages from the server for this chatroom
function showMessages(chatroomId) {
    $.ajax({
        type: 'GET',
        dataType: 'json',
        url: '/chats/' + chatroomId + '/message',
        success: function (data) {
            console.log('data: ', data);
            // 수정 부분: i < data.length
            for (let i = 0; i < data.length; i++) {
                // 수정 부분: showMessages -> showMessage (단일 메시지 표시)
                showMessage(data[i]);
            }
        },
        error: function (request, status, error) {
            console.log('request:', request);
            console.log('error:', error);
        }
    });
}

// Append incoming message to the chat area
function showMessage(chatMessage) {
    console.log("Received Message:", chatMessage);  // Debug log
    $("#messages").append(
        "<tr><td>" + (chatMessage.sender || "Unknown Sender") + " : " + chatMessage.message + "</td></tr>"
    );
}

// Create a new chatroom
function createChatroom() {
    $.ajax({
        type: 'POST',
        dataType: 'json',
        url: '/chats?title=' + $('#chatroom-title').val(),
        success: function (data) {
            console.log('data:', data);
            showChatrooms(0);
            enterChatrooms(data.id, true);
        },
        error: function (request, status, error) {
            console.log('request:', request);
            console.log('error:', error);
        }
    });
}

// Fetch and display all chatrooms
function showChatrooms(pageNumber) {
    $.ajax({
        type: 'GET',
        dataType: 'json',
        url: '/consultants/chats?sorts=id,desc&page=' + pageNumber,
        success: function (data) {
            console.log('data:', data);
            renderChatrooms(data);
        },
        error: function (request, status, error) {
            console.log('request:', request);
            console.log('error:', error);
        }
    });
}

// Render chatrooms as clickable table rows
function renderChatrooms(page) {

    let chatrooms = page.content;

    $("#chatroom-list").html("");
    for (let i = 0; i < chatrooms.length; i++) {
        $("#chatroom-list").append(
            "<tr onclick='joinChatroom(" + chatrooms[i].id + ")'>" +
            "<td>" + chatrooms[i].id + "</td>" +
            "<td>" + chatrooms[i].title + "<img src='new.png' id='new_" + chatrooms[i].id +
            "' style='display: " + getDisplayValue(chatrooms[i].hasNewMessage) + "'></td>" +
            // 여기서 memberCount 셀을 제대로 열고 닫음
            "<td id='memberCount_" + chatrooms[i].id + "'>" + chatrooms[i].memberCount + "</td>" +
            "<td>" + chatrooms[i].createAt + "</td>" +
            "</tr>"
        );
    }

    if (page.first) {
        $("#prev").prop("disabled", true);
    } else {
        $("#prev").prop("disabled", false).click(() => showChatrooms(page.number - 1))
    }

    if (page.last) {
        $("#next").prop("disabled", true);
    } else {
        $("#next").prop("disabled", false).click(() => showChatrooms(page.number + 1));
    }
}

function getDisplayValue(hasNewMessage) {
    if (hasNewMessage) {
        return "inline";
    }
    return "none";
}


// Enter a specific chatroom
function enterChatrooms(chatroomId, newMember) {
    $("#chatroom-id").val(chatroomId);
    $("#messages").html("");  // 메시지 목록 초기화
    showMessages(chatroomId); // chatroomId 넘겨서 메시지 가져오기

    $("#conversation").show();
    $("#send").prop("disabled", false);
    $("#leave").prop("disabled", false);

    toggleNewMessageIcon(chatroomId, false);

    // Unsubscribe if already subscribed
    if (subscription != null) {
        subscription.unsubscribe();
    }

    // Subscribe to this chatroom's channel
    subscription = stompClient.subscribe('/sub/chats/' + chatroomId, (chatMessage) => {
        showMessage(JSON.parse(chatMessage.body));
    });

    // If new member, send a "joined" message
    if (newMember) {
        stompClient.publish({
            destination: "/pub/chats/" + chatroomId,
            body: JSON.stringify({
                'message': "님이 방에 들어왔습니다."
            })
        });
    }
}

// Join a chatroom (call backend to register new membership)
function joinChatroom(chatroomId) {
    let currentChatroomId = $("#chatroom-id").val();

    $.ajax({
        type: 'POST',
        dataType: 'json',
        url: '/chats/' + chatroomId + getRequestParam(currentChatroomId),
        success: function (data) {
            console.log('data:', data);
            enterChatrooms(chatroomId, data);
        },
        error: function (request, status, error) {
            console.log('request:', request);
            console.log('error:', error);
        }
    });
}

function getRequestParam(currentChatroomId) {
    if (currentChatroomId == "") {
        return "";
    }
    return "?currentChatroomId=" + currentChatroomId;
}

// Leave a chatroom
function leaveChatroom() {
    let chatroomId = $("#chatroom-id").val();
    $.ajax({
        type: 'DELETE',
        dataType: 'json',
        url: '/chats/' + chatroomId,
        success: function (data) {
            console.log('data:', data);
            showChatrooms(0);
            exitChatroom(chatroomId);
        },
        error: function (request, status, error) {
            console.log('request:', request);
            console.log('error:', error);
        }
    });
}

// Clear out the chatroom UI
function exitChatroom(chatroomId) {
    $("#chatroom-id").val("");
    $("#conversation").hide();
    $("#send").prop("disabled", true);
    $("#leave").prop("disabled", true);

    if (subscription != null) {
        subscription.unsubscribe();
        subscription = null;
    }
}

// jQuery DOM ready
$(function () {
    $("form").on('submit', (e) => e.preventDefault());
    $("#connect").click(() => connect());
    $("#disconnect").click(() => disconnect());
    $("#send").click(() => sendMessage());
    $("#create").click(() => createChatroom());
    $("#leave").click(() => leaveChatroom());
});
