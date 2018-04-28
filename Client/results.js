$.get("http://localhost:4567/search/" + localStorage.getItem("search"), function (succ) {
    // $( ".result" ).html( data );
    console.log(JSON.parse(succ));
    results = document.getElementById("resultsPage");
    succ=JSON.parse(succ);
    for(obj in succ){
        l="<div class=\"search-results\">"+
            "<h2><a href=\""+succ[obj].url+"\">"+succ[obj].title+"</a></h2>"+
        "<a><a style = \"color:green\">"+succ[obj].url+"</a></a>"+
        "<p class=\"summary\">"+succ[obj].snippet+"</p>"+
        "</div>";
        $('#resultsPage').append(l);
        // $('#resultsPage').append("<div class = \"new_row\">"+
        //     "<label>country </label>"+
        //     "<input type= \"input\" name = \"country\">"+
        //     "<label>IPs </label>"+
        //     "<input type= \"input\" name = \"IPs\">"+
        //     "</div>");
    }

});


sendDelayedKeyPress = function (key) {
    console.log("first");
    document.getElementById("search-bar").onkeypress = null;
    setTimeout(function () {
        console.log("here");
        console.log(key);
        getSearchSuggestions(document.getElementById("search-bar").value);
        document.getElementById("search-bar").onkeypress = sendDelayedKeyPress;
    }, 1000);
};
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
submitSearch=function(text){
    localStorage.setItem("search", document.getElementById("search-bar").value);
    window.location.href="results.html";
};
document.getElementById("search-bar").onkeypress=sendDelayedKeyPress;
document.getElementById("mag").onclick=submitSearch;