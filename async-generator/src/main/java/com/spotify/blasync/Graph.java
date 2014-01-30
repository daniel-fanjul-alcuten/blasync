package com.spotify.blasync;

import java.util.HashMap;
import java.util.HashSet;

class Graph {

	class WrapperInfo {
		final Wrapper<?> wrapper;
		boolean root;
		boolean future;

		WrapperInfo(Wrapper<?> wrapper) {
			super();
			this.wrapper = wrapper;
		}

		boolean isAsync() {
			return root || future;
		}
	}

	HashMap<Wrapper<?>, WrapperInfo> wrappers;

	Graph() {
		wrappers = new HashMap<Wrapper<?>, WrapperInfo>();
	}

	void add(Wrapper<?> wrapper) {

		if (this.wrappers.containsKey(wrapper)) {
			return;
		}

		if (wrapper instanceof IfWrapper) {
			IfWrapper<?> iwrapper = (IfWrapper<?>) wrapper;
			add(iwrapper.getCondWrapper());
			add(iwrapper.getThenWrapper());
			add(iwrapper.getElseWrapper());

		} else if (wrapper instanceof DelegatedWrapper) {
			DelegatedWrapper<?> dwrapper = (DelegatedWrapper<?>) wrapper;
			add(dwrapper.getWrapper());
			for (Wrapper<?> w : dwrapper.getArgs()) {
				add(w);
			}

		} else if (wrapper instanceof LiteralWrapper) {

		} else if (wrapper instanceof PojoWrapper) {
			PojoWrapper<?> pwrapper = (PojoWrapper<?>) wrapper;
			for (Wrapper<?> w : pwrapper.getArgs()) {
				add(w);
			}
			for (Wrapper<?> w : pwrapper.getSetters().values()) {
				add(w);
			}

		} else if (wrapper instanceof CastWrapper) {
			CastWrapper<?> cwrapper = (CastWrapper<?>) wrapper;
			add(cwrapper.getWrapper());

		} else if (wrapper instanceof EqualsWrapper) {
			EqualsWrapper ewrapper = (EqualsWrapper) wrapper;
			add(ewrapper.getWrapper1());
			add(ewrapper.getWrapper2());

		} else if (wrapper instanceof DefaultWrapper) {
			// TODO(dfanjul): DefaultWrapper is super class of all instances,
			// find alternative

		} else {
			throw new UnsupportedOperationException("Unknown Wrapper class "
					+ wrapper.getClass().getName());
		}

		this.wrappers.put(wrapper, new WrapperInfo(wrapper));
	}

	class GroupInfo {
		final WrapperInfo root;
		final HashSet<Wrapper<?>> nodes;
		final HashSet<Wrapper<?>> deps;

		GroupInfo(WrapperInfo root) {
			this.root = root;
			this.nodes = new HashSet<Wrapper<?>>();
			this.deps = new HashSet<Wrapper<?>>();
			this.add(root.wrapper);
		}

		private void add(Wrapper<?> wrapper) {

			if (this.nodes.contains(wrapper)) {
				return;
			}

			if (wrapper != this.root.wrapper && wrappers.get(wrapper).isAsync()) {
				this.deps.add(wrapper);
				return;
			}

			if (wrapper instanceof IfWrapper) {
				IfWrapper<?> iwrapper = (IfWrapper<?>) wrapper;
				add(iwrapper.getCondWrapper());
				add(iwrapper.getThenWrapper());
				add(iwrapper.getElseWrapper());

			} else if (wrapper instanceof DelegatedWrapper) {
				DelegatedWrapper<?> dwrapper = (DelegatedWrapper<?>) wrapper;
				add(dwrapper.getWrapper());
				for (Wrapper<?> w : dwrapper.getArgs()) {
					add(w);
				}

			} else if (wrapper instanceof LiteralWrapper) {

			} else if (wrapper instanceof PojoWrapper) {
				PojoWrapper<?> pwrapper = (PojoWrapper<?>) wrapper;
				for (Wrapper<?> w : pwrapper.getArgs()) {
					add(w);
				}
				for (Wrapper<?> w : pwrapper.getSetters().values()) {
					add(w);
				}

			} else if (wrapper instanceof CastWrapper) {
				CastWrapper<?> cwrapper = (CastWrapper<?>) wrapper;
				add(cwrapper.getWrapper());

			} else if (wrapper instanceof EqualsWrapper) {
				EqualsWrapper ewrapper = (EqualsWrapper) wrapper;
				add(ewrapper.getWrapper1());
				add(ewrapper.getWrapper2());

			} else if (wrapper instanceof DefaultWrapper) {
				// TODO(dfanjul): DefaultWrapper is super class of all
				// instances, find alternative

			} else {
				throw new UnsupportedOperationException(
						"Unknown Wrapper class " + wrapper.getClass().getName());
			}

			this.nodes.add(wrapper);
		}
	}

	HashMap<Wrapper<?>, GroupInfo> groups;

	HashMap<Wrapper<?>, GroupInfo> calculateGroups() {
		this.groups = new HashMap<Wrapper<?>, GroupInfo>();

		for (WrapperInfo info : this.wrappers.values()) {
			if (info.isAsync()) {
				this.groups.put(info.wrapper, new GroupInfo(info));
			}
		}

		for (GroupInfo info1 : groups.values()) {
			for (GroupInfo info2 : groups.values()) {
				if (info1 != info2) {
					HashSet<Wrapper<?>> intersection = new HashSet<Wrapper<?>>(
							info1.nodes);
					intersection.retainAll(info2.nodes);
					if (!intersection.isEmpty()) {
						throw new UnsupportedOperationException(
								"Not empty intersection is not implemented yet");
					}
				} else {
					break;
				}
			}
		}

		return this.groups;
	}
}
