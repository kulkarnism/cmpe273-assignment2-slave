$(":button").click(function() { 
    var isbn = this.id; 
    var myStatusId = "#status"+isbn;
    var buttonId="#"+isbn;
    $.ajax({
            url: '/library/v1/books/'+isbn+"?status=lost", 
            type: 'PUT', 
    success: function() { 
            $(myStatusId).text("lost");
            $(buttonId).prop("disabled",true);         
            } 
            }); 
    });