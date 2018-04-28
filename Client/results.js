$.get("http://localhost:4567/search/" + localStorage.getItem("search"), function (succ) {
    // $( ".result" ).html( data );
    console.log(JSON.parse(succ));
    results = document.getElementById("resultsPage");
    succ=JSON.parse(succ);
    for(obj in succ){
        l="<div class=\"search-results\">"+
            "<h2><a href=\""+succ[obj].url+"\">"+succ[obj].title+"</a></h2>"+
        "<p>"+succ[obj].url+"</p>"+
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
    document.getElementById("searchbar").onkeypress = null;
    setTimeout(function () {
        console.log("here");
        console.log(key);
        getSearchSuggestions(document.getElementById("searchbar").value);
        document.getElementById("searchbar").onkeypress = sendDelayedKeyPress;
    }, 1000);
};

var input = document.getElementById("searchbar");