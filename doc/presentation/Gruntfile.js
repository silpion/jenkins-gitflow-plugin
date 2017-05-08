/* global module:false */
module.exports = function(grunt) {
	var port = grunt.option('port') || 8000;
	var root = grunt.option('root') || '.';

	if (!Array.isArray(root)) root = [root];

	// Project configuration
	grunt.initConfig({
		pkg: grunt.file.readJSON('package.json'),

        jshint: {
            options: {
                curly: false,
                eqeqeq: true,
                immed: true,
                esnext: true,
                latedef: true,
                newcap: true,
                noarg: true,
                sub: true,
                undef: true,
                eqnull: true,
                browser: true,
                expr: true,
                globals: {
                    head: false,
                    module: false,
                    console: false,
                    unescape: false,
                    define: false,
                    exports: false
                }
            },
            files: [ 'Gruntfile.js', 'js/reveal.js' ]
        },

        connect: {
            server: {
                options: {
                    port: port,
                    base: root,
                    livereload: true,
                    open: true
                }
            }

        },

        watch: {
            js: {
                files: [ 'Gruntfile.js', 'js/reveal.js' ],
                tasks: 'js'
            },
            theme: {
                files: [ 'css/theme/source/*.scss', 'css/theme/template/*.scss' ],
                tasks: 'css-themes'
            },
            css: {
                files: [ 'css/reveal.scss' ],
                tasks: 'css-core'
            },
            options: {
                livereload: true
            }

        }
    });

    // Dependencies
    grunt.loadNpmTasks( 'grunt-contrib-qunit' );
    grunt.loadNpmTasks( 'grunt-contrib-jshint' );
    grunt.loadNpmTasks( 'grunt-contrib-cssmin' );
    grunt.loadNpmTasks( 'grunt-contrib-uglify' );
    grunt.loadNpmTasks( 'grunt-contrib-watch' );
    grunt.loadNpmTasks( 'grunt-sass' );
    grunt.loadNpmTasks( 'grunt-contrib-connect' );
    grunt.loadNpmTasks( 'grunt-autoprefixer' );
    grunt.loadNpmTasks( 'grunt-zip' );
    grunt.loadNpmTasks( 'grunt-retire' );

    // Serve presentation locally
    grunt.registerTask( 'serve', [ 'connect', 'watch' ] );

};
