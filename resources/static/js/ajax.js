$(window).on('load', function () {
		$('.selectpicker').selectpicker({
				'selectedText': 'cat'
		});
});

$(document).ready(function() {
	if(location.hash) {
		$('a[href=' + location.hash + ']').tab('show');
	}
	$(document.body).on("click", "a[data-toggle]", function(event) {
		location.hash = this.getAttribute("href");
	});
	if($("#num_unresolved_bets").html() > 0) {
		setInterval(function(){updateUnresolvedBets();}, 5000);
	}
});

$(window).on('popstate', function() {
	var anchor = location.hash || $("a[data-toggle=tab]").first().attr("href");
	$('a[href=' + anchor + ']').tab('show');
});

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
			window.location.reload(true);
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

function getCardRank(card) {
	if (card.charAt(0) == "T") {
		return 10;
	} else {
		return card.charAt(0);
	}
}
function getCardSuit(card) {
	if (card.charAt(1) == "D") {
		return "diams";
	} else if (card.charAt(1) == "H") {
		return "hearts";
	} else if (card.charAt(1) == "S") {
		return "spades";
	} else if (card.charAt(1) == "C") {
		return "clubs";
	}
}
