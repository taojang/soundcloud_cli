try {
    require("source-map-support").install();
} catch(err) {
}
require("./out/goog/bootstrap/nodejs.js");
require("./out/soundcloud_cli.js");
goog.require("soundcloud_cli.core");
goog.require("cljs.nodejscli");
