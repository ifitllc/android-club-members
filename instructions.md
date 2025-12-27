Build an adroid app with Supabase to manage club memberships. 

Screens:
1. Admin login with Supabase Auth
2. Active members list screen with name, expiration, and avatar.
    2.1.  double tap the member to enter edit/renewal screen, allow to update email, phone or photo, or payment, that will extend expiration
    2.2.  the screen shall have an add new member button, that will navigate to add new screen
3. Search expired member by name, email or phone, double tap the found member to enter edit/renewal screen
4. Create avatar service, that will take photo or pick from photo gallary, image shall be compressed before uploading to supabase storage/bucket
5. all offline mode. 
6. setting screen show have a offline data status, allow to sync with supabase (members and their photos), allow bidirectional sync when network is ready


I have set up supabase:

tables:
    members:
       columns:  id (int8) primary key;   created_at. timestamptz default now(); name varchar, email varchar, phone varchar, expiration date, uid UUID, avatar_url, updated_at timestamptz default now();

    payments:
       columns: id (int8) primary key;   created_at. timestamptz default now(); amount numeric, member_id int8 reference members.id
    
 
 storage butcket: avatars

 