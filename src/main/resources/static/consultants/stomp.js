// ----------------------------------------------------
// 1) 전역 변수
// ----------------------------------------------------
let subscription = null;
const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/stomp/chats'
});

// ----------------------------------------------------
// 2) STOMP Client 설정
// ----------------------------------------------------
stompClient.onConnect = (frame) => {
    setConnected(true);
    showChatrooms(0);
    // 상담사 전용으로 모든 chatroom update를 수신
    stompClient.subscribe('/sub/chats/updates', (chatMessage) => {
        const chatObj = JSON.parse(chatMessage.body);
        toggleNewMessageIcon(chatObj.id, true);
        updateMemberCount(chatObj);
    });
    console.log('Connected: ' + frame);
};

stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
};
stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
};

// ----------------------------------------------------
// 3) 버튼/요소 제어
// ----------------------------------------------------
function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    $("#create").prop("disabled", !connected);
}

function connect() {
    stompClient.activate();
}

function disconnect() {
    stompClient.deactivate();
    setConnected(false);
    console.log("Disconnected");
}

// ----------------------------------------------------
// 4) 메시지 관련 함수
// ----------------------------------------------------
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

function showMessages(chatroomId) {
    $.ajax({
        type: 'GET',
        dataType: 'json',
        url: '/chats/' + chatroomId + '/message',
        success: function (data) {
            console.log('Messages data: ', data);
            $("#messages").html("");
            for (let i = 0; i < data.length; i++) {
                showMessage(data[i]);
            }
        },
        error: function (req, status, err) {
            console.log('Error showMessages:', err);
        }
    });
}

function showMessage(chatMessage) {
    $("#messages").append(
        "<tr><td>" + (chatMessage.senderName || "Unknown") + " : " + chatMessage.message + "</td></tr>"
    );
}

// ----------------------------------------------------
// 5) 채팅방 관련
// ----------------------------------------------------
function createChatroom() {
    $.ajax({
        type: 'POST',
        dataType: 'json',
        url: '/chats?title=' + $('#chatroom-title').val(),
        success: function (data) {
            console.log('Created chatroom:', data);
            showChatrooms(0);
            enterChatrooms(data.id, true);
        },
        error: function (req, status, err) {
            console.log('Error createChatroom:', err);
        }
    });
}

function showChatrooms(pageNumber) {
    $.ajax({
        type: 'GET',
        dataType: 'json',
        url: '/consultants/chats?sorts=id,desc&page=' + pageNumber,
        success: function (data) {
            renderChatrooms(data);
        },
        error: function (req, status, err) {
            console.log('Error showChatrooms:', err);
        }
    });
}

function renderChatrooms(page) {
    let chatrooms = page.content;
    $("#chatroom-list").html("");
    for (let i = 0; i < chatrooms.length; i++) {
        $("#chatroom-list").append(
            "<tr onclick='joinChatroom(" + chatrooms[i].id + ")'>" +
            "<td>" + chatrooms[i].id + "</td>" +
            "<td>" + chatrooms[i].title + "<img src='new.png' id='new_" + chatrooms[i].id +
            "' style='display: " + getDisplayValue(chatrooms[i].hasNewMessage) + "'></td>" +
            "<td id='memberCount_" + chatrooms[i].id + "'>" + chatrooms[i].memberCount + "</td>" +
            "<td>" + chatrooms[i].createAt + "</td>" +
            "</tr>"
        );
    }

    if (page.first) {
        $("#prev").prop("disabled", true);
    } else {
        $("#prev").prop("disabled", false).off('click').on('click', () => showChatrooms(page.number - 1));
    }

    if (page.last) {
        $("#next").prop("disabled", true);
    } else {
        $("#next").prop("disabled", false).off('click').on('click', () => showChatrooms(page.number + 1));
    }
}

function getDisplayValue(hasNewMessage) {
    return hasNewMessage ? "inline" : "none";
}

function toggleNewMessageIcon(chatroomId, toggle) {
    if (chatroomId == $("#chatroom-id").val()) {
        return;
    }
    if (toggle) {
        $("#new_" + chatroomId).show();
    } else {
        $("#new_" + chatroomId).hide();
    }
}

function updateMemberCount(chatroom) {
    $('#memberCount_' + chatroom.id).html(chatroom.memberCount);
}

// ----------------------------------------------------
// 6) 방 입장/퇴장
// ----------------------------------------------------
function joinChatroom(chatroomId) {
    let currentChatroomId = $("#chatroom-id").val();
    $.ajax({
        type: 'POST',
        dataType: 'json',
        url: '/chats/' + chatroomId + (currentChatroomId ? "?currentChatroomId=" + currentChatroomId : ""),
        success: function (data) {
            console.log('joinChatroom:', data);
            enterChatrooms(chatroomId, data);  // data == true|false
        },
        error: function (req, status, err) {
            console.log('Error joinChatroom:', err);
        }
    });
}

function enterChatrooms(chatroomId, newMember) {
    $("#chatroom-id").val(chatroomId);
    $("#messages").html("");
    showMessages(chatroomId);

    $("#conversation").show();
    $("#send").prop("disabled", false);
    $("#leave").prop("disabled", false);

    toggleNewMessageIcon(chatroomId, false);

    if (subscription != null) {
        subscription.unsubscribe();
    }
    subscription = stompClient.subscribe('/sub/chats/' + chatroomId, (chatMessage) => {
        showMessage(JSON.parse(chatMessage.body));
    });

    if (newMember) {
        stompClient.publish({
            destination: "/pub/chats/" + chatroomId,
            body: JSON.stringify({
                'message': "님이 방에 들어왔습니다."
            })
        });
    }
}

function leaveChatroom() {
    let chatroomId = $("#chatroom-id").val();
    $.ajax({
        type: 'DELETE',
        dataType: 'json',
        url: '/chats/' + chatroomId,
        success: function (data) {
            console.log('leaveChatroom:', data);
            showChatrooms(0);
            exitChatroom(chatroomId);
        },
        error: function (req, status, err) {
            console.log('Error leaveChatroom:', err);
        }
    });
}

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

// ----------------------------------------------------
// 7) 페이지 로드 시 초기화
// ----------------------------------------------------
$(function () {
    $("form").on('submit', (e) => e.preventDefault());
    $("#connect").click(() => connect());
    $("#disconnect").click(() => disconnect());
    $("#send").click(() => sendMessage());
    $("#create").click(() => createChatroom());
    $("#leave").click(() => leaveChatroom());
});