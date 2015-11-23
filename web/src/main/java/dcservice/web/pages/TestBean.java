package dcservice.web.pages;

import common.exceptions.PersistenceBeanException;
import dcservice.persistence.Action;
import dcservice.persistence.TransactionExecuter;
import dcservice.persistence.dao.DaoManager;
import dcservice.web.BaseEditBean;
import dcservice.web.common.HeaderBean;
import dcservice.web.wrappers.FieldWrapper;
import models.FieldResponse;
import models.base.BaseModel;
import models.fields.Field;
import models.fields.QField;
import models.responses.Response;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static common.helpers.ValidationHelper.isNullOrEmpty;

@ManagedBean
@ViewScoped
public class TestBean extends BaseEditBean implements Serializable {

	public static final long serialVersionUID = 7186971769033070058L;

	public List<FieldWrapper> questions;

	public boolean dataSubmited;

	private void readObject(ObjectInputStream ois)
			throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
	}

	private void readObjectNoData() throws ObjectStreamException {
		throw new InvalidObjectException("Stream data required");
	}
	
	@Override
	protected void onConstruct() {
			List<Field> fields = new ArrayList<Field>(0);
			try {
				fields = DaoManager.query().from(QField.field)
						.where(QField.field.active.eq(true)).list(QField.field);
			} catch (IllegalAccessException | PersistenceBeanException e) {
				log.error(e.getMessage(), e);
			}
			questions = new ArrayList<FieldWrapper>(0);
			fields.forEach(item -> {
				questions.add(new FieldWrapper(item));
				item = null;
			});
			dataSubmited = false;
	}

	@Override
	public void onSubmit() {
		cleanValidation();
		try {
			TransactionExecuter.execute(new Action() {
				@Override
				public void execute() {
					BaseModel response = new Response();
					DaoManager.save(response);
					questions.forEach(question -> {
						FieldResponse fieldResponse = new FieldResponse();
						try {
							fieldResponse.setField(DaoManager.query()
									.from(QField.field)
									.where(QField.field.id.eq(question.getId()))
									.uniqueResult(QField.field));
						} catch (IllegalAccessException | PersistenceBeanException e) {
							log.error(e.getMessage(), e);
						}
						fieldResponse.setResponse((Response) response);
						switch (question.getType()) {
							case COMBOBOX:
							case SINGLE_LINE_TEXT:
							case TEXTAREA:
							case RADIO_BUTTON:
								fieldResponse.setAnswer(question.getAnswer());

							case DATE:
								fieldResponse.setAnswer(question.getDateAnswer()
										.toString());

							case CHECKBOX:
								fieldResponse.setAnswer(String.valueOf(question
										.isBooleanAnswer()));
						}
						DaoManager.save(fieldResponse);

					});
				}
			});
			HeaderBean.updateResponsesCount();
		} catch (IllegalAccessException | PersistenceBeanException e) {
			log.error(e.getMessage(), e);
		}
		
	}

	@Override
	protected void afterSave() {
		dataSubmited = true;
		updateJS("questions_panel");
	}

	public void validate() {
		questions.forEach(question -> {
			if (question.getRequired())
				switch (question.getType()) {
				case TEXTAREA:
				case SINGLE_LINE_TEXT:
				case COMBOBOX:
				case RADIO_BUTTON:
					if (isNullOrEmpty(question.getAnswer())) {
						addException("testRequiredFields");
					}
					break;

				case DATE:
					if (isNullOrEmpty(question.getDateAnswer())) {
						addException("testRequiredFields");
					}
					break;
				case CHECKBOX:
					if (isNullOrEmpty(question.isBooleanAnswer())) {
						addException("testRequiredFields");
					}
					break;
				}
		});
	}

	public List<FieldWrapper> getQuestions() {
		return questions;
	}

	public void setQuestions(List<FieldWrapper> questions) {
		this.questions = questions;
	}

	public boolean isDataSubmited() {
		return dataSubmited;
	}

	public void setDataSubmited(boolean dataSubmited) {
		this.dataSubmited = dataSubmited;
	}

}
