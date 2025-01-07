let subscription = null;
const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/stomp/chats'
});

stompClient.onConnect = (frame) => {
    setConnected(true);
    // 일반 유저가 로그인하자마자 본인이 참여한 채팅방 목록 불러오기
    loadMyChatrooms();
    stompClient.subscribe('/sub/chats/updates', (chatMessage) => {
        // 방에 새 메시지 알림
        const chatObj = JSON.parse(chatMessage.body);
        toggleNewMessageIcon(chatObj.id, true);
        updateMemberCount(chatObj);
    });
    console.log('Connected: ' + frame);
};

stompClient.onWebSocketError = (error) => {
    console.error('WebSocket Error:', error);
};
stompClient.onStompError = (frame) => {
    console.error('STOMP Error:', frame);
};

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    $("#send").prop("disabled", !connected);
}

function connect() {
    stompClient.activate();
}

function disconnect() {
    stompClient.deactivate();
    setConnected(false);
}

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

function loadMyChatrooms() {
    // 일반 유저가 참여한 채팅방 목록 (ex: /chats GET)
    $.ajax({
        type: 'GET',
        dataType: 'json',
        url: '/chats',
        success: function (data) {
            renderMyChatrooms(data);
        },
        error: function (req, status, err) {
            console.log('Error loadMyChatrooms:', err);
        }
    });
}

function renderMyChatrooms(chatrooms) {
    $("#chatroom-list").html("");
    for (let i = 0; i < chatrooms.length; i++) {
        let c = chatrooms[i];
        $("#chatroom-list").append(`
                    <tr onclick="joinChatroom(${c.id})">
                        <td>${c.id}</td>
                        <td>${c.title}
                            <img src="new.png" id="new_${c.id}" style="display:${c.hasNewMessage ? 'inline':'none'}" />
                        </td>
                        <td id="memberCount_${c.id}">${c.memberCount}</td>
                        <td>${c.createAt}</td>
                    </tr>
                `);
    }
}

function joinChatroom(chatroomId) {
    let currentChatroomId = $("#chatroom-id").val();
    $.ajax({
        type: 'POST',
        dataType: 'json',
        url: '/chats/' + chatroomId + (currentChatroomId ? "?currentChatroomId="+currentChatroomId : ""),
        success: function (data) {
            console.log('joinChatroom:', data);
            enterChatroom(chatroomId, data);
        },
        error: function (req, status, err) {
            console.log('Error joinChatroom:', err);
        }
    });
}

function enterChatroom(chatroomId, newMember) {
    $("#chatroom-id").val(chatroomId);
    $("#messages").html("");
    showMessages(chatroomId);

    if (subscription != null) {
        subscription.unsubscribe();
    }
    subscription = stompClient.subscribe('/sub/chats/' + chatroomId, (chatMessage) => {
        showMessage(JSON.parse(chatMessage.body));
    });

    toggleNewMessageIcon(chatroomId, false);

    if (newMember) {
        stompClient.publish({
            destination: "/pub/chats/" + chatroomId,
            body: JSON.stringify({
                'message': "님이 들어왔습니다."
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
            $("#chatroom-id").val("");
            $("#messages").html("");
            if (subscription != null) {
                subscription.unsubscribe();
                subscription = null;
            }
            loadMyChatrooms();
        },
        error: function (req, status, err) {
            console.log('Error leaveChatroom:', err);
        }
    });
}

function showMessages(chatroomId) {
    $.ajax({
        type: 'GET',
        dataType: 'json',
        url: '/chats/' + chatroomId + '/message',
        success: function (data) {
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

function showMessage(msg) {
    $("#messages").append(`
                <tr>
                    <td>${msg.senderName} : ${msg.message}</td>
                </tr>
            `);
}

function toggleNewMessageIcon(chatroomId, toggle) {
    if (chatroomId == $("#chatroom-id").val()) return;
    if (toggle) {
        $("#new_"+chatroomId).show();
    } else {
        $("#new_"+chatroomId).hide();
    }
}

function updateMemberCount(chatroom) {
    $('#memberCount_' + chatroom.id).html(chatroom.memberCount);
}

$(function(){
    $("form").on('submit',(e)=>e.preventDefault());
    $("#connect").click(()=>connect());
    $("#disconnect").click(()=>disconnect());
    $("#send").click(()=>sendMessage());
    $("#leave").click(()=>leaveChatroom());
});
