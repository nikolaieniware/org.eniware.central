<sec:authorize access="!hasRole('ROLE_USER')">
	<c:url value='/index.do' var='homeUrl'/>
</sec:authorize>
<sec:authorize access="hasRole('ROLE_USER')">
	<c:url value='/u/sec/home' var='homeUrl'/>
</sec:authorize>
<nav class="navbar navbar-default">
	<div class="container-fluid">
		<div class="navbar-header">
			<button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#eniwareuser-navbar-collapse-1">
				<span class="sr-only">Toggle navigation</span>
				<span class="icon-bar"></span>
				<span class="icon-bar"></span>
				<span class="icon-bar"></span>
			</button>
			<a class="navbar-brand" href="${homeUrl}">
				<img src="<c:url value='/img/logo.svg'/>" alt="<fmt:message key='app.name'/>" width="214" height="30"/>	
			</a>
	    </div>
	    
	    <div class="collapse navbar-collapse" id="eniwareuser-navbar-collapse-1">
			<ul class="nav navbar-nav">
				<li ${navloc == 'home' ? 'class="active"' : ''}><a href="${homeUrl}"><fmt:message key='link.home'/></a></li>				
				<sec:authorize access="!hasRole('ROLE_USER')">
					<li ${navloc == 'login' ? 'class="active"' : ''}>
						<a href="<c:url value='/login.do'/>"><fmt:message key="link.login"/></a>
					</li>
				</sec:authorize>
				<sec:authorize access="hasRole('ROLE_USER')">
					<li ${navloc == 'my-Edges' ? 'class="active"' : ''}>
						<a href="<c:url value='/u/sec/my-Edges'/>"><fmt:message key="link.my-Edges"/></a>
					</li>
					<li ${navloc == 'alerts' ? 'class="active"' : ''}>
						<a href="<c:url value='/u/sec/alerts'/>"><fmt:message key="link.alerts"/> <span class="label label-danger label-as-badge alert-situation-count"></span></a>
					</li>
					<li ${navloc == 'auth-tokens' ? 'class="active"' : ''}>
						<a href="<c:url value='/u/sec/auth-tokens'/>"><fmt:message key="link.auth-tokens"/></a>
					</li>
					<sec:authorize access="hasRole('ROLE_BILLING')">
						<li ${navloc == 'billing' ? 'class="active"' : ''}>
							<a href="<c:url value='/u/sec/billing'/>"><fmt:message key="link.billing"/></a>
						</li>
					</sec:authorize>
				</sec:authorize>
	 		</ul>
	        
			<sec:authorize access="hasRole('ROLE_USER')">
				<ul class="nav navbar-nav navbar-right">
					<li class="dropdown">
						<a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-expanded="false">
							<fmt:message key='nav.label.principal'>
								<fmt:param><sec:authentication property="principal.username" /></fmt:param>
							</fmt:message>
							<b class="caret"></b>
						</a>
						<ul class="dropdown-menu">
							<li  ${navloc == 'profile' ? 'class="active"' : ''}>
								<a href="<c:url value='/u/sec/profile'/>"><fmt:message key="link.profile"/></a>
							</li>
							<li><a class="logout" href="#"><fmt:message key='link.logout'/></a></li>
						</ul>
					</li>
				</ul>
				<form id="logout-form" method="post" action="<c:url value='/logout'/>">
					<sec:csrfInput/>
				</form>
			</sec:authorize>
	    </div>
	</div>
</nav>
