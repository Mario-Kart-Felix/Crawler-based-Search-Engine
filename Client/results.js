$.get("http://localhost:4567/search/" + localStorage.getItem("search"), function (succ) {
    // $( ".result" ).html( data );
    console.log(JSON.parse(succ));
    results = document.getElementById("resultsPage");
    succ=JSON.parse(succ);
    console.log(JSON.stringify(succ));
    for(obj in succ){
        l="<div class=\"search-results\">"+
            "<h2><a href=\"#\">"+obj["title"]+"</a></h2>"+
        "<p><a href=\"#\" class=\"link\">"+obj.url+"</a><p>"+
        "<p class=\"summary\">"+obj.snippet+"</p>"+
        "</div>";
        results.append(l);
    }
    alert( succ );
});

