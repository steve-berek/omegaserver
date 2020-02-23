cordova.define("cordova-plugin-omegaserver.OmegaServer", function(require, exports, module) {
    // Empty constructor
    var exec = require('cordova/exec');
    var  OmegaServer = function(){};
   
   // The function that passes work along to native shells
   // Message is a string, duration may be 'long' or 'short'
   OmegaServer.prototype.startServer = function(root, port,  successCallback, errorCallback) {
     var options = {};
     options.root = root;
     options.port = port;
     exec(successCallback, errorCallback, 'OmegaServer', 'startServer', [options]);
   }
   OmegaServer.prototype.stopServer = function(successCallback, errorCallback) {
     var options = {};
     exec(successCallback, errorCallback, 'OmegaServer', 'stopServer', [options]);
   }
   
   
   module.exports = new OmegaServer();
   // Installation constructor that binds ToastyPlugin to window
   OmegaServer.install = function() {
     if (!window.plugins) {
       window.plugins = {};
     }
     window.plugins.omegaServer = new OmegaServer();
     return window.plugins.omegaServer;
   };
   cordova.addConstructor(OmegaServer.install); 
   });
   