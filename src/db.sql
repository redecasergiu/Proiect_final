drop database chatdb;
create DATABASE if not exists chatdb;
use chatdb;


create table users(
	id int(6) primary key auto_increment not null,
    name varchar(100) not null unique, -- username
    hpass varchar(256),	-- hashed password
    salt varchar(256)	-- salt for the password
);


-- friends table
-- a,b are friends iff friends(a,b) and friends(b,a)
-- a friend request adds an entry in this table
create table friends(
	userid int(6),
	friendid int(6)
);



create table conversations(
	id int(6) primary key auto_increment not null,
	name varchar(256) -- conversation name
);


-- messages from the conversation
create table messages(
	conversationid int(6) not null,
	userid int(6) not null,	-- id of the sender
	content varchar(8192) not null -- message content
	-- isread boolean default false -- true if the 
);


-- permissions of users in the conversations
create table permissions(
	conversationid int(6) not null,
	userid int(6) not null,	-- id of the user of whom permission is set up
	r boolean default true,
	w boolean default true,
	x boolean default false, -- execute operations on the conversation (delete, add another user,.. )
	
	primary key(conversationid,userid)
);



--- Inserts
insert into users(name, hpass, salt) values
('jan', '56ab6a9c08aaafb8d7923e286163839aa660bbe66b19820ca1c31ef4672c1e90', 'feWcVFYwXgxf0o8U49Wp6j7fh9c0gkRND7uy0zBsm5Trrkv2UJctQJhHEpASk2HI375CNK2B03aswtxFZhPHKRvYG2Rh5rbdfTllbhHVco9Xm3FgqvForlVm4sSHi6m0kr4SVNFWZqCNtauaaNlwQJRO2IR9bLruHdinrbyQYwO7mVcv5oc4YFHMWPdI8zvr5k1ECjNmC2FyFZU8nM2JCqY4wnYtG0PEn8YejM8v4xoAd67uSIV8rpwBove0bwXv'),	-- jan:1234
('asd', '56ab6a9c08aaafb8d7923e286163839aa660bbe66b19820ca1c31ef4672c1e90', 'feWcVFYwXgxf0o8U49Wp6j7fh9c0gkRND7uy0zBsm5Trrkv2UJctQJhHEpASk2HI375CNK2B03aswtxFZhPHKRvYG2Rh5rbdfTllbhHVco9Xm3FgqvForlVm4sSHi6m0kr4SVNFWZqCNtauaaNlwQJRO2IR9bLruHdinrbyQYwO7mVcv5oc4YFHMWPdI8zvr5k1ECjNmC2FyFZU8nM2JCqY4wnYtG0PEn8YejM8v4xoAd67uSIV8rpwBove0bwXv'),	-- asd:1234
('ana', '56ab6a9c08aaafb8d7923e286163839aa660bbe66b19820ca1c31ef4672c1e90', 'feWcVFYwXgxf0o8U49Wp6j7fh9c0gkRND7uy0zBsm5Trrkv2UJctQJhHEpASk2HI375CNK2B03aswtxFZhPHKRvYG2Rh5rbdfTllbhHVco9Xm3FgqvForlVm4sSHi6m0kr4SVNFWZqCNtauaaNlwQJRO2IR9bLruHdinrbyQYwO7mVcv5oc4YFHMWPdI8zvr5k1ECjNmC2FyFZU8nM2JCqY4wnYtG0PEn8YejM8v4xoAd67uSIV8rpwBove0bwXv');	-- ana:1234





-- create conversation
-- RETURN: conversation id
drop function if exists createConversation;
delimiter //
create function createConversation(_userid int(6), _name varchar(256)) returns int(6)
	not deterministic
begin
    set @myid = null;
	
	insert into conversations(name) values
	(_name);
 
    select id as 'id' into @myid
	from conversations
	order by id desc
	limit 1;
	
	call editConversationPermissions(@myid, _userid, true, true, true);	-- add all permissions for the conversation initiator
 
	return (@myid);
end //
delimiter ;






-- add conversation permission
-- usedin: createConversation[f]
drop procedure if exists  editConversationPermissions;
delimiter //
create procedure editConversationPermissions(_conversationid int(6), _userid int(6), _r boolean, _w boolean, _x boolean) -- add consultation details
begin 
	delete
	from permissions
	where permissions.conversationid = _conversationid and permissions.userid = _userid;
	
	insert into permissions(conversationid, userid, r, w, x) values
	(_conversationid, _userid, _r, _w, _x);
end //
delimiter ;







-- register user
-- RETURN: user id or -1 if the user already exists
drop function if exists registerUser;
delimiter //
create function registerUser(_name varchar(100), _hpass varchar(256), _salt varchar(256)) returns int(6)
	not deterministic
begin
    set @usid = null;	-- user id
	
	select id into @usid
	from users
	where users.name = _name;
	
	if @usid is null then	-- if the user already exists
		insert into users(name, hpass, salt) values
		(_name, _hpass, _salt); -- register user
		select id into @usid
		from users
		where _name = users.name
		order by id desc;
		return (@usid);
	else
		return (-1);
	end if;
 
	
end //
delimiter ;







-- RETURN: hpass and salt of the user
drop procedure if exists getCredentials;
delimiter //
create procedure getCredentials(_name varchar(100))
begin
	
	select id, hpass, salt
	from users
	where users.name = _name;

end //
delimiter ;





-- RETURN: conversations of a user (conversation ids and conversation names)
drop procedure if exists getConversations;
delimiter //
create procedure getConversations(_id int(6))
begin
	
	select conversations.id, conversations.name
	from conversations, permissions
	where conversations.id = permissions.conversationid and permissions.userid = _id;

end //
delimiter ;





-- store a message of the conversation
drop procedure if exists storeMessage;
delimiter //
create procedure storeMessage(_conversationid int(6), _userid int(6), _content varchar(8192))
begin
	
	insert into messages(conversationid, userid, content) values 
	(_conversationid, _userid, _content);

	call getConversationUsers(_conversationid);
	
end //
delimiter ;







-- RETURN: users(owners of the message) and messages of the conversation
drop procedure if exists getMessages;
delimiter //
create procedure getMessages(_conversationid int(6))
begin

	select users.name as 'username' , messages.content as 'content'
	from messages, users
	where messages.conversationid = _conversationid and messages.userid = users.id;
	
end //
delimiter ;






-- RETURN: participants of a conversation
drop procedure if exists getParticipants;
delimiter //
create procedure getParticipants(_conversationid int(6))
begin

	select users.name as 'username'
	from permissions, users
	where permissions.conversationid = _conversationid and permissions.userid = users.id;
	
end //
delimiter ;



-- add a participant
drop procedure if exists addParticipant;
delimiter //
create procedure addParticipant(_conversationid int(6), _participantname varchar(100))
begin
	set @participantid = null;
	
	select users.id into @participantid
	from users
	where users.name = _participantname;

	insert into permissions(conversationid, userid) values
	(_conversationid, @participantid);
	
end //
delimiter ;




-- add a participant
drop procedure if exists getConversationUsers;
delimiter //
create procedure getConversationUsers(_conversationid int(6))
begin

	select userid as 'userid'
	from permissions
	where permissions.conversationid = _conversationid; -- and permissions.r = true;
	
end //
delimiter ;
