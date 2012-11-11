yepnope({
    load: ['lib/jquery-1.8.2.min.js', 'bs/css/bootstrap.min.css', 'bs/js/bootstrap.min.js', 'lib/custom-web/date.js', 'lib/custom-web/cross-utils.js', 'lib/common-web/underscore-min.js', 'lib/common-web/underscore.strings.js', 'css/whiskey2.css', 'lib/lima1/net.js', 'lib/lima1/main.js'],
    complete: ->
        $(document).ready(() ->
            app = new Whiskey2
            app.start()
        );
})

class Whiskey2
    start: ->
        db = new HTML5Provider 'app.db', '1'
        storage = new StorageProvider db
        # 'http://localhost:8888'
        jqnet = new jQueryTransport 'http://lima1-kvj.rhcloud.com'
        oauth = new OAuthProvider {
            clientID: 'whiskey2web'
        }, jqnet
        manager = new Lima1DataManager 'whiskey2', oauth, storage
        oauth.token = manager.get('token')