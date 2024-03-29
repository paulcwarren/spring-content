package internal.org.springframework.content.jpa.testsupport.models;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class Claim {

	@Id
	//@GeneratedValue(strategy = GenerationType.IDENTITY)
	@org.springframework.data.annotation.Id
	private String claimId = UUID.randomUUID().toString();

	private String lastName;
	private String firstName;
	
	@Embedded
	private ClaimForm claimForm = new ClaimForm();

	public String getClaimId() {
		return claimId;
	}

	public void setClaimId(String claimId) {
		this.claimId = claimId;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public ClaimForm getClaimForm() {
		return claimForm;
	}

	public void setClaimForm(ClaimForm claimForm) {
		this.claimForm = claimForm;
	}
}
