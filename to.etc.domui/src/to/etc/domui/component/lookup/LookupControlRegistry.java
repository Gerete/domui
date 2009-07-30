package to.etc.domui.component.lookup;

import java.util.*;

import to.etc.domui.component.input.*;
import to.etc.domui.component.input.ComboFixed.*;
import to.etc.domui.component.meta.*;
import to.etc.domui.component.meta.impl.*;
import to.etc.domui.dom.html.*;
import to.etc.domui.util.*;
import to.etc.webapp.nls.*;
import to.etc.webapp.query.*;

/**
 * Default Registry of Lookup control factories.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Jul 23, 2008
 */
public class LookupControlRegistry {
	private List<LookupControlFactory> m_factoryList = new ArrayList<LookupControlFactory>();

	public LookupControlRegistry() {
		register(TEXT_CF);
		register(DATE_CF);
		register(NUMERIC_CF);
		register(new RelationLookupFactory());
		register(new LookupControlFactoryEnumAndBool());
	}

	public synchronized List<LookupControlFactory> getFactoryList() {
		return m_factoryList;
	}

	public synchronized void register(LookupControlFactory f) {
		m_factoryList = new ArrayList<LookupControlFactory>(m_factoryList);
		m_factoryList.add(f);
	}

	public LookupControlFactory findFactory(PropertyMetaModel pmm) {
		LookupControlFactory best = null;
		int score = 0;
		for(LookupControlFactory cf : m_factoryList) {
			int v = cf.accepts(pmm);
			if(v > score) {
				score = v;
				best = cf;
			}
		}
		return best;
	}

	public LookupControlFactory getControlFactory(PropertyMetaModel pmm) {
		LookupControlFactory cf = findFactory(pmm);
		if(cf == null)
			throw new IllegalStateException("Cannot get a Lookup Control factory for " + pmm);
		return cf;
	}

	/**
	 * Default factory for most non-relational fields. This treats the property as a convertable
	 * text input thingy.
	 */
	@SuppressWarnings("unchecked")
	static public final LookupControlFactory TEXT_CF = new LookupControlFactory() {
		public LookupFieldQueryBuilderThingy createControl(final SearchPropertyMetaModel spm, final PropertyMetaModel pmm) {
			Class<?> iclz = pmm.getActualType();

			//-- Boolean/boolean types? These need a tri-state checkbox
			if(iclz == Boolean.class || iclz == Boolean.TYPE) {
				throw new IllegalStateException("I need a tri-state checkbox component to handle boolean lookup thingies.");
			}

			//-- Treat everything else as a String using a converter.
			final Text< ? > txt = new Text(iclz);
			if(pmm.getDisplayLength() > 0)
				txt.setSize(pmm.getDisplayLength());
			else {
				//-- We must decide on a length....
				int sz = 0;
				if(pmm.getLength() > 0) {
					sz = pmm.getLength();
					if(sz > 40)
						sz = 40;
				}
				if(sz != 0)
					txt.setSize(sz);
			}
			if(pmm.getConverterClass() != null)
				txt.setConverterClass((Class) pmm.getConverterClass());
			if(pmm.getLength() > 0)
				txt.setMaxLength(pmm.getLength());

			//-- Converter thingy is known. Now add a
			return new DefaultLookupThingy(txt) {
				@Override
				public boolean appendCriteria(QCriteria crit) throws Exception {
					Object value = null;
					try {
						value = txt.getValue();
					} catch(Exception x) {
						return false; // Has validation error -> exit.
					}
					if(value == null || (value instanceof String && ((String) value).trim().length() == 0))
						return true; // Is okay but has no data

					// FIXME Handle minimal-size restrictions on input (search field metadata


					//-- Put the value into the criteria..
					if(value instanceof String) {
						String str = (String) value;
						str = str.trim() + "%";
						crit.ilike(pmm.getName(), str);
					} else {
						crit.eq(pmm.getName(), value); // property == value
					}
					return true;
				}
			};
		}

		public int accepts(PropertyMetaModel pmm) {
			return 1; // Accept all properties (will fail on incompatible ones @ input time)
		}
	};


	static public final LookupControlFactory DATE_CF = new LookupControlFactory() {

		public LookupFieldQueryBuilderThingy createControl(SearchPropertyMetaModel spm, final PropertyMetaModel pmm) {
			final DateInput df = new DateInput();
			TextNode tn = new TextNode(NlsContext.getGlobalMessage(Msgs.UI_LOOKUP_DATE_TILL));
			final DateInput dt = new DateInput();
			return new DefaultLookupThingy(df, tn, dt) {
				@Override
				public boolean appendCriteria(QCriteria< ? > crit) throws Exception {
					Date from, till;
					try {
						from = df.getValue();
					} catch(Exception x) {
						return false;
					}
					try {
						till = dt.getValue();
					} catch(Exception x) {
						return false;
					}
					if(from == null && till == null)
						return true;
					if(from != null && till != null) {
						if(from.getTime() > till.getTime()) {
							//-- Swap vals
							df.setValue(till);
							dt.setValue(from);
							from = till;
							till = dt.getValue();
						}

						//-- Between query
						crit.between(pmm.getName(), from, till);
					} else if(from != null) {
						crit.ge(pmm.getName(), from);
					} else {
						crit.lt(pmm.getName(), till);
					}
					return true;
				}
			};
		}

		public int accepts(PropertyMetaModel pmm) {
			if(Date.class.isAssignableFrom(pmm.getActualType()))
				return 2;
			return 0;
		}
	};

	static public final LookupControlFactory NUMERIC_CF = new LookupControlFactory() {
		public LookupFieldQueryBuilderThingy createControl(SearchPropertyMetaModel spm, final PropertyMetaModel pmm) {
			final List<Pair<NumericRelationType>> values = new ArrayList<Pair<NumericRelationType>>();
			for(NumericRelationType relationEnum : NumericRelationType.values()) {
				values.add(new Pair<NumericRelationType>(relationEnum, MetaManager.findClassMeta(NumericRelationType.class).getDomainLabel(NlsContext.getLocale(), relationEnum)));
			}

			final Text< ? > numA = createNumericInput(pmm);
			final Text< ? > numB = createNumericInput(pmm);
			numB.setDisabled(true);

			final ComboFixed<NumericRelationType> relationCombo = new ComboFixed<NumericRelationType>(values);

			final DefaultLookupThingy result = new DefaultLookupThingy(relationCombo, numA, numB) {
				@Override
				public boolean appendCriteria(QCriteria< ? > crit) throws Exception {
					NumericRelationType relation;
					relation = relationCombo.getValue();
					if(relation == null) {
						return true;
					}
					if(!numA.validate()) {
						return false;
					}
					if(relation == NumericRelationType.BETWEEN && !numB.validate()) {
						return false;
					}
					switch(relation){
						case EQ:
							crit.eq(pmm.getName(), numA.getValue());
							break;
						case LT:
							crit.lt(pmm.getName(), numA.getValue());
							break;
						case LE:
							crit.le(pmm.getName(), numA.getValue());
							break;
						case GT:
							crit.gt(pmm.getName(), numA.getValue());
							break;
						case GE:
							crit.ge(pmm.getName(), numA.getValue());
							break;
						case NOT_EQ:
							crit.ne(pmm.getName(), numA.getValue());
							break;
						case BETWEEN:
							crit.between(pmm.getName(), numA.getValue(), numB.getValue());
							break;
					}
					return true;
				}
			};

			relationCombo.setClicked(new IClicked<ComboFixed<NumericRelationType>>() {

				public void clicked(ComboFixed<NumericRelationType> b) throws Exception {
					if(b.getValue() == NumericRelationType.BETWEEN) {
						if(numB.isDisabled()) {
							numB.setDisabled(false);
						}
					} else if(!numB.isDisabled()) {
						numB.setDisabled(true);
						numB.setValue(null);
					}
				}

			});
			return result;
		}

		@SuppressWarnings("unchecked")
		private Text< ? > createNumericInput(final PropertyMetaModel pmm) {
			Class< ? > iclz = pmm.getActualType();

			//-- Create first text control that accept any numeric type.
			final Text< ? > numText = new Text(iclz);
			/*
			 * Length calculation using the metadata. This uses the "length" field as LAST, because it is often 255 because the
			 * JPA's column annotation defaults length to 255 to make sure it's usability is bloody reduced. Idiots.
			 */
			if(pmm.getDisplayLength() > 0)
				numText.setSize(pmm.getDisplayLength());
			else if(pmm.getPrecision() > 0) {
				// FIXME This should be localized somehow...
				//-- Calculate a size using scale and precision.
				int size = pmm.getPrecision();
				int d = size;
				if(pmm.getScale() > 0) {
					size++; // Inc size to allow for decimal point or comma
					d -= pmm.getScale(); // Reduce integer part,
					if(d >= 4) { // Can we get > 999? Then we can have thousand-separators
						int nd = (d - 1) / 3; // How many thousand separators could there be?
						size += nd; // Increment input size with that
					}
				}
				numText.setSize(size);
			} else if(pmm.getLength() > 0) {
				numText.setSize(pmm.getLength() < 40 ? pmm.getLength() : 40);
			}

			if(pmm.getConverterClass() != null)
				numText.setConverterClass((Class) pmm.getConverterClass());
			if(pmm.getLength() > 0)
				numText.setMaxLength(pmm.getLength());
			String s = pmm.getDefaultHint();
			if(s != null)
				numText.setTitle(s);
			for(PropertyMetaValidator mpv : pmm.getValidators())
				numText.addValidator(mpv);

			return numText;
		}

		public int accepts(PropertyMetaModel pmm) {
			//-- Return a low value; special format input line monetary needs different factory?
			if(Integer.class == pmm.getActualType() || Double.class == pmm.getActualType() || pmm.getActualType() == double.class) {
				return 2;
			}
			return 0;
		}
	};

}
