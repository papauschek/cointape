

requirejs.config({
    //baseUrl: 'libs',
    shim: {
        bootstrap: { deps: ['jquery'] }
    },
    paths: {
        //app: '../app',
        bootstrap: 'libs/bootstrap.min',
        jquery: 'libs/jquery-2.1.4.min',
        react: 'libs/react-0.13.2',
        'react-bootstrap': 'libs/react-bootstrap-0.20.3',
        'react-router': 'libs/react-router'
    }
});

// Start loading the main app file. Put all of
// your application logic in there.
requirejs(['app/main']);