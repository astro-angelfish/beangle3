[#ftl]
[@b.head title="Dashboard"]
	<link href="${base}/static/themes/default/css/panel.css" rel="stylesheet" type="text/css"/>
	<style type="text/css">
	img {border:0 none;vertical-align:middle}
	.module{margin:0 3 5px;line-height:1.3em;float:left;width:42%;}
	</style>
[/@]

[#macro menuitem image="" link="" name="" remark=""]
<tr>
	<td><a href="${link}"><img height="48" style="margin-right:1em" alt="" width="48" src="${base}/static/icons/beangle/48x48/${image!}" /></a></td>
	<td style="vertical-align:middle">
		<div class="link"><a href="${link}">${name}</a></div>
		<div style="color:gray; text-decoration:none;">${remark!}</div>
	</td>
</tr>
[/#macro]

<h1 style="padding-left: 1em;">系统权限管理</h1>
<table>
<tr>
	<td width="40%" valign="top">
		<table id="management-links" style="padding-left: 2em;">
			[@menuitem image="setting.gif" link="${b.url('!admin')}" name="权限配置" remark="规划用户分类,指定超级管理员"/]
			[@menuitem image="user.gif" link="${b.url('user')}" name="用户管理" remark="创建用户,修改密码,分配用户组"/]
			[@menuitem image="group.gif" link="${b.url('group')}" name="用户组管理" remark="创建用户组,修改用户组,维护组内用户,分配资源权限和数据权限"/]
			[@menuitem image="menu.gif" link="${b.url('menu')}" name="菜单资源" remark="注册/启用/禁用资源,创建菜单配置,维护菜单项,启用/禁用菜单"/]
			[@menuitem image="monitor.gif" link="${b.url('monitor')}" name="系统监控" remark="管理在线用户,控制在线数量,查询登录历史"/]
		</table>
	</td>
	<td valign="top">
	[@sj.div id="data" href="${b.url('!stat')}" /]
	</td>
</tr>
</table>

[@b.foot/]
