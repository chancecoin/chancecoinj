function updateChatStatus() {
	$.ajax({
		url: "/chat_status_update?chat_open="+$('#chat_open').html(),
		type: 'GET',
		success: function(data) {
		}
	});
}

function processBet(formName) {
	processAjaxRequest("/process_bet", "#"+formName, "Processing your bet, please wait...");
}

function processImportPrivateKey(formName) {
	processAjaxRequest("/process_import_private_key", "#"+formName, "Importing your private key...");
}

function processSend(formName) {
	processAjaxRequest("/process_send", "#"+formName, "Processing your sending transaction...");
}

function processAjaxRequest(urlString, formName, waitMessage) {
	$.ajax({
		type: "POST",
		url: urlString,
		data: $(formName).serialize(),
		beforeSend: function() {
			$(formName+" input").prop("disabled", true);
			$(formName+" button").prop("disabled", true);
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
			$(formName+" input").prop("disabled", false);
			$(formName+" button").prop("disabled", false);
			$(formName+" input").val("");
			if (urlString=="/process_bet") {
				window.location = "/casino";
			} else {
				window.location.reload(true);
			}
		}
	});
}

function updateUnresolvedBets() {
	$.ajax({
		type: "POST",
		url: "/update_unresolved_bets",
		success: function(response) {
			if (response < $("#num_unresolved_bets").html()) {
				window.location.reload(true);
			} else {
				$("#num_unresolved_bets").html(response);
			}
		}
	});
}
