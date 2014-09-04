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

function getCasinoInfo() {
	$.ajax({
		type: "GET",
		url: "http://0.0.0.0:8080/get_casino_info",
		crossDomain: true,
		success: function(response) {
			var responseObj = jQuery.parseJSON(response);
			var betInfo;
			var html = "";
			html += "<table class='table table-striped'>"
			html += "<thead>";
			html +=	"<tr>";
			html +=	"<th>Source address</th>";
			html +=	"<th>Time</th>";
			html +=	"<th>Bet size</th>";
			html +=	"<th>Chance to win / payout multiplier</th>";
			html +=	"<th>Result</th>";
			html +=	"<th>Profit</th>";
			html +=	"</tr>";
			html +=	"</thead>";

			html += "<tbody>";

	 		for (betInfo of responseObj.bets) {
				html += "<tr>";
				html += "<td>"+betInfo["source"].substring(0,6)+"...</td>";
				html += "<td>"+betInfo["block_time"]+"</td>";
				html += "<td>"+betInfo["bet"]+" CHA</td>";
				html += "<td>"+parseFloat(betInfo["chance"].toPrecision(3))+"%"+" / "+parseFloat(betInfo["payout"].toPrecision(3))+"X</td>";

				if (betInfo["cards"]) {
					html += "<td>";
					var cardArray = betInfo["cards"].split(" ");
					for (var cardIndex = 0; cardIndex < cardArray.length; cardIndex++) {
							if (cardIndex == 0) {
									html += "<div style='float: left; padding-top: 1.25em; padding-right: 2.5em;'>Player</div>";
							}
							if (cardIndex == 7) {
									html += "<div style='float: left; padding-top: 1.25em; padding-right: 1em;'>Opponent</div>";
							}
							if (cardArray[cardIndex] == "??") {
									html += "<div class='card back'>*</div>";
							} else {
									html += "<div class='card rank-"+getCardRank(cardArray[cardIndex])+" "+getCardSuit(cardArray[cardIndex])+"'>";
									html += "<span class='rank'>"+getCardRank(cardArray[cardIndex])+"</span>";
									html +=	"<span class='suit'>&"+getCardSuit(cardArray[cardIndex])+";</span>";
									html += "</div>";
							}
							if (cardIndex==1 || cardIndex==6) {
									html += "<div style='clear: both;'></div>";
							}
					}
					if (betInfo["cards_result"]) {
							html += "<p>"+betInfo['cards_result']+"</p>";
					}
					html += "</td>";
				} else {
					html += "<td><img src='http://chancecoin.com/images/dice.png' style='height: 25px; display: inline;' />";
					if (betInfo["resolved"]) {
						html += betInfo["roll"];
					} else {
						html += "?";
					}
					html += "</td>";
				}
				html += "<td>";
				if (betInfo["resolved"] && betInfo["resolved"]=="true") {
					html += betInfo["profit"]+" CHA";
				} else {
					html += "<img src='http://chancecoin.com/images/ajax-loader.gif' /></td>";
				}

				html += "</tr>";
			}
			html += "</tbody>";
			html += "</table>";

			$("#bets_content").html(html);

		}
	});
}
