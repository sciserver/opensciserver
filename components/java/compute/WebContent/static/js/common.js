/*
  Copyright (c) Johns Hopkins University. All rights reserved.
  Licensed under the Apache License, Version 2.0.
  See LICENSE.txt in the project root for license information.
*/
$(function() {
	$('ul.nav > li > a').each(function() {
		const link = $(this);
		const badget = link.children('span');
		if (link.text() === 'Long Running Tasks ') {
			$.get({
				url: $('#page-navbar').data('racm') + '/jobm/rest/jobs?open=true',
				headers: {
					'X-Auth-Token': Cookies.get('computeCookie')
				}
			}).then(function (data) {
				badget.text(data.length);
				badget.prop('title', data.length + " on-going tasks");
			});
		}
	});
});