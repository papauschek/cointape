
define(['react', 'react-router', 'jquery', 'bootstrap',
        'libs/underscore'],
    function(React, ReactRouter) {

    // setup jquery
    $.ajaxSetup({contentType: 'application/json'});

    return {
        React: React,
        ReactRouter: ReactRouter
    };

});