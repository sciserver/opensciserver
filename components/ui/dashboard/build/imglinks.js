const fs = require('fs')
const glob = require('glob')
const config = require('../config')
const path = require('path')

const imgdir = path.join(config.build.assetsRoot, config.build.assetsSubDirectory, "img")
process.chdir(imgdir)

// Make img files available for reference by other components.  These files must not be inlined by webpack.  The
// threshold for this is found in 'url-loader' config for images in webpack.base.conf.js

const imgFiles = ['sciserver_banner.jpg', 'sciserverlogo.png', 'casjobs.png', 'scidrive.png', 'sciserver_compute.png',
                  'sciserver_compute_jobs.png', 'sciserver_logo_icon_blue.png', 'skyquery.png', 'skyserver.png']

let globFile = ''
let globFiles = []
imgFiles.forEach(function(imgFile) {
    // Modify file name to accommodate hash added by webpack
    globFile = imgFile.replace(/\.([^.]*)$/, '.*.$1')
    globFiles = glob.sync(globFile)
    fs.copyFileSync(globFiles[0], imgFile)
})
