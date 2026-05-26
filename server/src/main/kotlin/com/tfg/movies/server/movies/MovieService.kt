package com.tfg.movies.server.movies

import com.tfg.movies.shared.movies.MovieDetails

/**
 * Business logic for movies. Thin today (delegates to repository) but
 * the natural place for validations, caching, and composition later.
 */
class MovieService(
    private val repository: MovieRepository,
) {
    fun getById(id: Int): MovieDetails? = repository.findById(id)
}