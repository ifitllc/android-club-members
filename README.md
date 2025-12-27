# Android Club Members

Supabase-backed Android app (Kotlin, Jetpack Compose, Room, Hilt) for managing club memberships with offline-first cache, avatar upload, and bidirectional sync.

## Features
- Supabase Auth email/password admin login.
- Active members list, add, edit/renew, and expired search (name/email/phone). Double-tap to open edit/renew.
- Avatar picker (camera/gallery via system picker), JPEG compression before upload to Supabase Storage bucket `avatars`.
- Offline cache via Room; local edits persist offline and sync to Supabase when `Sync now` is triggered.
- Settings shows cache status and last sync trigger.

## Project structure
- `app/`: Android app module
  - `data/db`: Room entities, DAO, database
  - `data/network`: Supabase DTOs and client provider
  - `data/repo`: Repositories with offline/online sync and storage upload
  - `viewmodel`: Hilt view models for screens
  - `ui/screens`: Compose screens and navigation
  - `util`: Image compression helper

## Supabase setup
1. Table schema (matches your setup):
```sql
create table if not exists public.members (
  id bigint primary key,
  created_at timestamptz default now(),
  name varchar not null,
  email varchar,
  phone varchar,
  expiration date,
  uid uuid,
  avatar_url text,
  updated_at timestamptz default now(),
  is_deleted boolean default false
);

create table if not exists public.payments (
  id bigint primary key,
  created_at timestamptz default now(),
  amount numeric,
  member_id bigint references public.members(id)
);

alter table public.members enable row level security;
create policy if not exists "Authenticated read" on public.members for select using (auth.role() = 'authenticated');
create policy if not exists "Authenticated write" on public.members for insert with check (auth.role() = 'authenticated');
create policy if not exists "Authenticated update" on public.members for update using (auth.role() = 'authenticated');
```
2. Storage bucket `avatars` (public recommended); the list screen builds public URLs as `SUPABASE_URL/storage/v1/object/public/avatars/{avatar_url}`. If private, switch to signed URLs via `MemberRepository.getAvatarUrl`.
3. In `local.properties`, add your Supabase keys:
```
supabaseUrl=https://YOUR-PROJECT.supabase.co
supabaseAnonKey=YOUR_ANON_KEY
```

## Build & run
1. Ensure Android Studio Giraffe/AGP 8.2+ and JDK 17.
2. (Optional) generate Gradle wrapper: `gradle wrapper --gradle-version 8.4`.
3. Build/run from Android Studio or `./gradlew :app:assembleDebug`.

## Sync model
- Room is source for UI. `MemberRepository.addOrUpdate` writes locally first, then tries to push to Supabase (fails gracefully if offline). "Sync now" pulls remote rows and keeps the newest `updated_at` per row.
- Avatar uploads are best-effort; if offline upload fails, the member is saved without a new avatar until next edit.

## Notes / follow-ups
- Add background periodic WorkManager sync for automatic reconnection.
- Hook camera capture (currently uses gallery picker). Add runtime permission handling if enabling camera.
- Strengthen conflict resolution (e.g., field-level merge) and queued push retries.
- Harden input validation and add tests.
