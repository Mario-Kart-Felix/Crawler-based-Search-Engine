getSearchSuggestions = function (text) {
    console.log("http://localhost:4567/suggestion/" + text);
    $.get("http://localhost:4567/suggest/" + text, function (succ) {
        // $( ".result" ).html( data );
        $(function () {

                var obj = JSON.parse(succ);
                var sugg = obj.Suggestions;
            var availableTags = sugg;
                console.log(sugg);
                $("#searchbar").autocomplete({
                    source: availableTags
                });
            }
        );
        // alert( succ );
    });
};
sendDelayedKeyPress = function (key) {
    console.log("first");
    document.getElementById("searchbar").onkeypress = null;
    setTimeout(function () {
        console.log("here");
        console.log(key);
        getSearchSuggestions(document.getElementById("searchbar").value);
        document.getElementById("searchbar").onkeypress = sendDelayedKeyPress;
    }, 1000);
};

var input = document.getElementById("searchbar");


document.getElementById("searchbar").onkeypress = sendDelayedKeyPress;