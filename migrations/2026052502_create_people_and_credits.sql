-- ============================================================================
-- Migration: 2026052502_create_people_and_credits
-- Purpose:   Create tables for persons (actors, directors, etc) and their
--            roles in movies (cast and crew).
--
-- Design notes:
--   - 'people' is a single table for both actors and crew members. The
--     same person can appear in both cast and crew roles across films.
--   - movie_cast and movie_crew are separate tables because they have
--     different attributes: cast has character_name and cast_order,
--     crew has department and job.
--   - Primary keys are composite (movie_id, person_id, ...). This
--     allows a person to appear multiple times in the same movie:
--     - In cast: same actor playing two characters (cast_order distinguishes)
--     - In crew: same person with multiple jobs (e.g. writer + director)
--   - All references to movies and people use ON DELETE CASCADE so the
--     credit rows disappear when the parent is removed.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- people: actors, directors, producers, etc. One row per person.
-- ----------------------------------------------------------------------------
CREATE TABLE people (
                        id              INTEGER     PRIMARY KEY,
                        name            TEXT        NOT NULL,
                        gender          SMALLINT    NULL,
                        profile_path    TEXT        NULL
);

COMMENT ON TABLE people IS
    'Persons appearing in films, either as cast or crew members. TMDB id
     used as primary key.';

COMMENT ON COLUMN people.gender IS
    'TMDB gender encoding: 0=not specified, 1=female, 2=male, 3=non-binary.';

CREATE INDEX idx_people_name ON people (lower(immutable_unaccent(name)));

-- ----------------------------------------------------------------------------
-- movie_cast: many-to-many relation between movies and people, with role.
-- ----------------------------------------------------------------------------
CREATE TABLE movie_cast (
                            movie_id        INTEGER     NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                            person_id       INTEGER     NOT NULL REFERENCES people(id) ON DELETE CASCADE,
                            cast_order      SMALLINT    NOT NULL,
                            character_name  TEXT        NULL,
                            PRIMARY KEY (movie_id, person_id, cast_order)
);

COMMENT ON TABLE movie_cast IS
    'Actors appearing in each film. cast_order reflects billing order in
     credits (0 = lead). Same actor can appear multiple times in the same
     film (double roles) distinguished by cast_order.';

CREATE INDEX idx_movie_cast_person ON movie_cast (person_id);
CREATE INDEX idx_movie_cast_movie_order ON movie_cast (movie_id, cast_order);

-- ----------------------------------------------------------------------------
-- movie_crew: many-to-many relation between movies and people, with job.
-- ----------------------------------------------------------------------------
CREATE TABLE movie_crew (
                            movie_id    INTEGER     NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                            person_id   INTEGER     NOT NULL REFERENCES people(id) ON DELETE CASCADE,
                            job         TEXT        NOT NULL,
                            department  TEXT        NULL,
                            PRIMARY KEY (movie_id, person_id, job)
);

COMMENT ON TABLE movie_crew IS
    'Technical crew of each film. A person can hold multiple jobs in the
     same film (e.g. writer and director); they appear as separate rows.';

CREATE INDEX idx_movie_crew_person ON movie_crew (person_id);
CREATE INDEX idx_movie_crew_job ON movie_crew (job);