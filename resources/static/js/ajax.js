function updateChatStatus() {
	$.ajax({
		url: "/chat_status_update?chat_open="+$('#chat_open').html(),
		type: 'GET',
		success: function(data) {
		}
	});
}

function processBet() {
	processAjaxRequest("/process_bet", "#casino_bet_form", "processing your bet...");
}

function processImportPrivateKey() {
	processAjaxRequest("/process_import_private_key", "#import_private_key_form", "importing your private key...");
}

function processSend() {
	processAjaxRequest("/process_send", "#send_cha_form", "processing your sending transaction...");
}

function processAjaxRequest(urlString, formName, waitMessage) {
	$.ajax({
		type: "POST",
		url: urlString,
		data: $(formName).serialize(),
		beforeSend: function() {
			$("#ajax_result_message").css("display","none");
			$("#ajax_result_message").html("");
			$("#ajax_wait_message").html(waitMessage);
			$("#ajax_wait_message").css("display","block");
			$("#ajax_message").css("display","block");
		},
		success: function(response) {
			var responseObj = jQuery.parseJSON(response);
			$("#ajax_wait_message").css("display","none");
			$("#ajax_wait_message").html("");
			$("#ajax_result_message").html(responseObj.message);
			$("#ajax_result_message").css("display","block");
			$("#ajax_message").delay(3000);
			$("#ajax_message").fadeOut(500);
		}
	});
}
