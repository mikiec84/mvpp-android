package com.apiumhub.github.list

import com.apiumhub.github.core.domain.BaseInteractor
import com.apiumhub.github.core.domain.BaseService
import com.apiumhub.github.core.domain.Event
import com.apiumhub.github.core.domain.entity.Repository
import io.reactivex.Scheduler
import io.reactivex.subjects.PublishSubject

interface RepositoryListService : BaseService {
  fun search(query: String)
  fun bindData(func: (List<Repository>) -> Unit)

  companion object {
    fun create(
      networkRepository: RepositoryListRepository,
      inMemoryRepository: RepositoryListRepository,
      observeOn: Scheduler,
      subscribeOn: Scheduler
    ): RepositoryListService =
      RepositoryListInteractor(
        networkRepository,
        inMemoryRepository,
        observeOn,
        subscribeOn
      )
  }
}

class RepositoryListInteractor(
  private val networkRepository: RepositoryListRepository,
  private val inMemoryRepository: RepositoryListRepository,
  observeOn: Scheduler,
  subscribeOn: Scheduler
) : BaseInteractor(observeOn, subscribeOn), RepositoryListService {

  private val successStream: PublishSubject<List<Repository>> = PublishSubject.create()

  override fun search(query: String) {
    if (query.isEmpty()) {
      findAll()
    } else {
      searchByQuery(query)
    }
  }

  private fun findAll() {
    execute(inMemoryRepository.findAllRepositories().flatMap {
      if (it.isNotEmpty()) successStream.onNext(it)
      networkRepository.findAllRepositories()
    }) {
      inMemoryRepository.addOrUpdateRepositories(it)
      successStream.onNext(it)
    }
  }

  private fun searchByQuery(query: String) {
    execute(inMemoryRepository.searchRepositories(query)
      .flatMap {
        if (it.items!!.isNotEmpty()) successStream.onNext(it.items)
        networkRepository.searchRepositories(query)
      }
    ) {
      inMemoryRepository.addOrUpdateRepositorySearch(query, it)
      when {
        it.items == null -> subject.onNext(Event.ERROR_NULL)
        it.items.isEmpty() -> subject.onNext(Event.EMPTY)
        else -> successStream.onNext(it.items)
      }
    }
  }

  override fun bindData(func: (List<Repository>) -> Unit) {
    disposeBag.add(successStream.subscribe { func(it) })
  }
}
